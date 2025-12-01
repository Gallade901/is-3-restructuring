import React, {useEffect, useMemo, useState, useRef} from 'react';
import {
    useReactTable,
    getCoreRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    getFilteredRowModel,
    flexRender
} from '@tanstack/react-table';
import { useNavigate } from 'react-router-dom';
import HeaderApp from "../../HeaderApp"

const Coordinates = () => {
    const userRole = localStorage.getItem("role");
    const login = localStorage.getItem("login");
    const [coordinatesData, setCoordinatesData] = useState([]);
    const [reconnectAttempts, setReconnectAttempts] = useState(0);
    const wsRef = useRef(null);
    const [xFilter, setXFilter] = useState("");
    const [yFilter, setYFilter] = useState("");
    const [replacementCoordinate, setReplacementCoordinate] = useState("")
    const reconnectTimeoutRef = useRef(null);

    useEffect(() => {
        const MAX_RECONNECT_ATTEMPTS = 10;
        const BASE_RECONNECT_DELAY = 1000;

        const startReconnect = () => {
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }

            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                console.error('Превышено максимальное количество попыток переподключения');
                return;
            }
            const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts), 30000);

            reconnectTimeoutRef.current = setTimeout(() => {
                console.log(`Попытка переподключения ${reconnectAttempts + 1} через ${delay}мс`);

                wsRef.current = new WebSocket(`${process.env.REACT_APP_WS_URL}/coordinate`);

                wsRef.current.onopen = () => {
                    console.log(`Успешное переподключение (попытка ${reconnectAttempts + 1})`);
                    setReconnectAttempts(0);
                };

                wsRef.current.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        setCoordinatesData(data);
                    } catch (error) {
                        console.error('Ошибка при обработке сообщения:', error);
                    }
                };

                wsRef.current.onerror = (error) => {
                    console.error('Ошибка WebSocket:', error);
                };

                wsRef.current.onclose = (event) => {
                    if (!event.wasClean) {
                        console.log('Соединение было прервано неожиданно');
                        setReconnectAttempts(prev => prev + 1);
                    } else {
                        console.log('Соединение было закрыто чисто');
                        setReconnectAttempts(0);
                    }
                };
            }, delay);
        };

        startReconnect();

        return () => {
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            if (wsRef.current) {
                wsRef.current.close();
            }
        };
    }, [reconnectAttempts]);

    useEffect(() => {
        if (coordinatesData && coordinatesData.length > 0 && replacementCoordinate === "") {
            handleCoordinateChange(coordinatesData[0].id);
        }
    }, [coordinatesData]);

    const navigate = useNavigate();

    const handleEdit = (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            navigate(`/editCoordinate/${id}`)
        }
    };

    const handleDelete = async (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            try {
                await fetch(`${process.env.REACT_APP_BASE_URL}/coordinates/${id}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(replacementCoordinate),
                    credentials: "include",
                });
            } catch (error) {
                console.error('Ошибка при удалении:', error);
            }
        }
    };

    const handleCoordinateChange = (e) => {
        let selectedCoordinateId;
        if (typeof e === 'object' && e.target) {
            // Вызов из select элемента
            selectedCoordinateId = e.target.value;
        } else {
            // Вызов из useEffect с прямым ID
            selectedCoordinateId = e;
        }
        const selectedCoordinate = coordinatesData.find(coordinate => coordinate.id === parseInt(selectedCoordinateId));
        setReplacementCoordinate(selectedCoordinate)
    };

    const filteredData = useMemo(() => {
        if (!coordinatesData) return [];
        return coordinatesData.filter(coordinate =>
            coordinate.x?.toString().includes(xFilter) &&
            coordinate.y?.toString().includes(yFilter)
        );
    }, [coordinatesData, xFilter, yFilter]);

    const columns = useMemo(() => [
        { accessorKey: 'id', header: 'ID' },
        { accessorKey: 'owner', header: 'Владелец'},
        {
            accessorKey: 'x',
            header: 'Координата X',
            cell: ({ getValue }) => {
                const value = getValue();
                return value?.toFixed(2) || '0.00';
            }
        },
        {
            accessorKey: 'y',
            header: 'Координата Y',
            cell: ({ getValue }) => {
                const value = getValue();
                return value?.toFixed(2) || '0.00';
            }
        },
        {
            id: 'edit',
            header: 'Изменить',
            cell: ({ row }) => (
                <button
                    onClick={() => handleEdit(row.original.id, row.original.owner)}
                    className="edit-button"
                >
                    Изменить
                </button>
            )
        },
        {
            id: 'delete',
            header: 'Удалить',
            cell: ({ row }) => (
                <button
                    onClick={() => handleDelete(row.original.id, row.original.owner)}
                    className="delete-button"
                    disabled={replacementCoordinate.id === row.original.id}
                >
                    Удалить
                </button>
            )
        }
    ], [replacementCoordinate]);

    const table = useReactTable({
        data: filteredData,
        columns,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        initialState: {
            pagination: {
                pageSize: 15,
                pageIndex: 0,
            },
        },
    });

    return (
        <div className="persons-table-wrapper">
            <HeaderApp/>
            <div className="filters">
                <input
                    type="text"
                    placeholder="Фильтр по X"
                    value={xFilter}
                    onChange={(e) => setXFilter(e.target.value)}
                />
                <input
                    type="text"
                    placeholder="Фильтр по Y"
                    value={yFilter}
                    onChange={(e) => setYFilter(e.target.value)}
                />
                <div>
                    Координаты по умолчанию для замены
                    <select onChange={handleCoordinateChange}>
                        {coordinatesData?.map((coordinate) => (
                            <option key={coordinate.id} value={coordinate.id}>
                                id-координаты-{coordinate.id}
                            </option>
                        ))}
                    </select>
                </div>
            </div>
            <div className="persons-table-wr">
                <table className="persons-table">
                    <thead>
                    {table.getHeaderGroups().map(headerGroup => (
                        <tr key={headerGroup.id}>
                            {headerGroup.headers.map(header => (
                                <th key={header.id}>
                                    {flexRender(header.column.columnDef.header, header.getContext())}
                                </th>
                            ))}
                        </tr>
                    ))}
                    </thead>
                    <tbody>
                    {table.getRowModel().rows.map(row => (
                        <tr key={row.id}>
                            {row.getVisibleCells().map(cell => (
                                <td key={cell.id}>
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </td>
                            ))}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            <div className="pagination">
                <button
                    onClick={() => table.previousPage()}
                    disabled={!table.getCanPreviousPage()}
                >
                    Previous
                </button>
                <span>Page{' '}
                    <strong>
                        {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                    </strong>{' '}
                </span>
                <button
                    onClick={() => table.nextPage()}
                    disabled={!table.getCanNextPage()}
                >
                    Next
                </button>
            </div>
        </div>
    );
};

export default Coordinates;