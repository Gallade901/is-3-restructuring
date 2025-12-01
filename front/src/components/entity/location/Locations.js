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

const Locations = () => {
    const userRole = localStorage.getItem("role");
    const login = localStorage.getItem("login");
    const [locationsData, setLocationsData] = useState([]);
    const [reconnectAttempts, setReconnectAttempts] = useState(0);
    const wsRef = useRef(null);
    const [xFilter, setXFilter] = useState("");
    const [yFilter, setYFilter] = useState("");
    const [zFilter, setZFilter] = useState("");
    const [replacementLocation, setReplacementLocation] = useState("")
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

                wsRef.current = new WebSocket(`${process.env.REACT_APP_WS_URL}/location`);

                wsRef.current.onopen = () => {
                    console.log(`Успешное переподключение (попытка ${reconnectAttempts + 1})`);
                    setReconnectAttempts(0);
                };

                wsRef.current.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        setLocationsData(data);
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
        if (locationsData && locationsData.length > 0 && replacementLocation === "") {
            handleLocationChange(locationsData[0].id);
        }
    }, [locationsData]);

    const navigate = useNavigate();

    const handleEdit = (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            navigate(`/editLocation/${id}`)
        }
    };

    const handleDelete = async (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            try {
                await fetch(`${process.env.REACT_APP_BASE_URL}/location/${id}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(replacementLocation),
                    credentials: "include",
                });
            } catch (error) {
                console.error('Ошибка при удалении:', error);
            }
        }
    };

    const handleLocationChange = (e) => {
        let selectedLocationId;
        if (typeof e === 'object' && e.target) {
            // Вызов из select элемента
            selectedLocationId = e.target.value;
        } else {
            // Вызов из useEffect с прямым ID
            selectedLocationId = e;
        }
        const selectedLocation = locationsData.find(location => location.id === parseInt(selectedLocationId));
        setReplacementLocation(selectedLocation)
    };

    const filteredData = useMemo(() => {
        if (!locationsData) return [];
        return locationsData.filter(location =>
            location.x?.toString().includes(xFilter) &&
            location.y?.toString().includes(yFilter) &&
            location.z?.toString().includes(zFilter)
        );
    }, [locationsData, xFilter, yFilter, zFilter]);

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
            accessorKey: 'z',
            header: 'Координата Z',
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
                    disabled={replacementLocation.id === row.original.id}
                >
                    Удалить
                </button>
            )
        }
    ], [replacementLocation]);

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
        <div className="locations-table-wrapper">
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
                <input
                    type="text"
                    placeholder="Фильтр по Z"
                    value={zFilter}
                    onChange={(e) => setZFilter(e.target.value)}
                />
                <div>
                    Локация по умолчанию для замены
                    <select onChange={handleLocationChange}>
                        {locationsData?.map((location) => (
                            <option key={location.id} value={location.id}>
                                id-локации-{location.id}
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

export default Locations;