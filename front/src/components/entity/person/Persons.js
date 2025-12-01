import React, {useEffect, useMemo, useState, useRef} from 'react';
import {
    useReactTable,
    getCoreRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    getFilteredRowModel, flexRender
} from '@tanstack/react-table';
import { useNavigate } from 'react-router-dom';
import HeaderApp from "../../HeaderApp"

const Persons = () => {
    const userRole = localStorage.getItem("role");
    const login = localStorage.getItem("login");
    const [personsData, setPersonsData] = useState();
    const [reconnectAttempts, setReconnectAttempts] = useState(0);
    const wsRef = useRef(null);
    const [nameFilter, setNameFilter] = useState("");
    const [nationalityFilter, setNationalityFilter] = useState("");
    const reconnectTimeoutRef = useRef(null);

    // useEffect(() => {
    //     const MAX_RECONNECT_ATTEMPTS = 10;
    //     const BASE_RECONNECT_DELAY = 1000; // 1 секунда
    //
    //     const startReconnect = () => {
    //         if (reconnectTimeoutRef.current) {
    //             clearTimeout(reconnectTimeoutRef.current);
    //         }
    //
    //         if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    //             console.error('Превышено максимальное количество попыток переподключения');
    //             return;
    //         }
    //         const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts), 30000);
    //
    //         reconnectTimeoutRef.current = setTimeout(() => {
    //             console.log(`Попытка переподключения ${reconnectAttempts + 1} через ${delay}мс`);
    //
    //             wsRef.current = new WebSocket(`${process.env.REACT_APP_WS_URL}/person`);
    //
    //             wsRef.current.onopen = () => {
    //                 console.log(`Успешное переподключение (попытка ${reconnectAttempts + 1})`);
    //                 setReconnectAttempts(0);
    //             };
    //
    //             wsRef.current.onmessage = (event) => {
    //                 try {
    //                     const data = JSON.parse(event.data);
    //                     setPersonsData(data);
    //                 } catch (error) {
    //                     console.error('Ошибка при обработке сообщения:', error);
    //                 }
    //             };
    //
    //             wsRef.current.onerror = (error) => {
    //                 console.error('Ошибка WebSocket:', error);
    //             };
    //
    //             wsRef.current.onclose = (event) => {
    //                 if (!event.wasClean) {
    //                     console.log('Соединение было прервано неожиданно');
    //                     setReconnectAttempts(prev => prev + 1);
    //                     // startReconnect();
    //                 } else {
    //                     console.log('Соединение было закрыто чисто');
    //                     setReconnectAttempts(0);
    //                 }
    //             };
    //         }, delay);
    //     };
    //
    //     startReconnect();
    //
    //     return () => {
    //         // Очищаем таймаут при размонтировании
    //         if (reconnectTimeoutRef.current) {
    //             clearTimeout(reconnectTimeoutRef.current);
    //         }
    //         // Закрываем WebSocket
    //         if (wsRef.current) {
    //             wsRef.current.close();
    //         }
    //     };
    // }, [reconnectAttempts]);

    // Вместо этого в useEffect:
    useEffect(() => {
        const MAX_RECONNECT_ATTEMPTS = 10;
        const BASE_RECONNECT_DELAY = 1000;

        const connectWebSocket = () => {
            if (wsRef.current?.readyState === WebSocket.OPEN) {
                return;
            }

            const ws = new WebSocket(`${process.env.REACT_APP_WS_URL}/person`);
            wsRef.current = ws;

            ws.onopen = () => {
                console.log('WebSocket connected');
                setReconnectAttempts(0);
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    setPersonsData(data);
                } catch (error) {
                    console.error('Ошибка при обработке сообщения:', error);
                }
            };

            ws.onerror = (error) => {
                console.error('WebSocket error:', error);
            };

            ws.onclose = (event) => {
                if (!event.wasClean && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts), 30000);
                    setTimeout(() => {
                        setReconnectAttempts(prev => prev + 1);
                        connectWebSocket();
                    }, delay);
                }
            };
        };

        connectWebSocket();

        return () => {
            if (wsRef.current) {
                wsRef.current.close();
            }
        };
    }, [reconnectAttempts]); // Только reconnectAttempts в зависимостях

    const navigate = useNavigate();

    const handleEdit = (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            navigate(`/editPerson/${id}`)
        }
    };

    const handleDelete = async (id, owner) => {
        if (owner === login || userRole === "ADMIN") {
            try {
                await fetch(`${process.env.REACT_APP_BASE_URL}/person/${id}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: "include",
                });
            } catch (error) {
                console.error('Ошибка при удалении:', error);
            }
        }
    };

    const filteredData = useMemo(() => {
        if (!personsData) return [];
        return personsData.filter(person =>
            person.name.toLowerCase().includes(nameFilter.toLowerCase()) &&
            (person.nationality ? person.nationality.toLowerCase().includes(nationalityFilter.toLowerCase()) : true)
        );
    }, [personsData, nameFilter, nationalityFilter]);

    const columns = useMemo(() => [
        { accessorKey: 'id', header: 'ID' },
        { accessorKey: 'owner', header: 'Владелец'},
        { accessorKey: 'name', header: 'Имя'},
        { accessorKey: 'coordinateX', header: 'Координата X' },
        { accessorKey: 'coordinateY', header: 'Координата Y' },
        { accessorKey: 'locationX', header: 'Локация X' },
        { accessorKey: 'locationY', header: 'Локация Y' },
        { accessorKey: 'locationZ', header: 'Локация Z' },
        { accessorKey: 'creationDate', header: 'Дата создания' },
        { accessorKey: 'eyeColor', header: 'Цвет глаз' },
        { accessorKey: 'hairColor', header: 'Цвет волос' },
        { accessorKey: 'height', header: 'Рост' },
        { accessorKey: 'birthday', header: 'День рождения' },
        { accessorKey: 'weight', header: 'Вес' },
        { accessorKey: 'nationality', header: 'Национальность' },
        {
            id: 'edit',
            header: 'Изменить',
            cell: ({ row }) => (
                <button onClick={() => handleEdit(row.original.id, row.original.owner)} className="edit-button">
                    Изменить
                </button>
            )
        },
        {
            id: 'delete',
            header: 'Удалить',
            cell: ({ row }) => (
                <button onClick={() => handleDelete(row.original.id, row.original.owner)} className="delete-button">
                    Удалить
                </button>
            )
        }
    ], []);

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
                    placeholder="Фильтр по имени"
                    value={nameFilter}
                    onChange={(e) => setNameFilter(e.target.value)}
                />
                <input
                    type="text"
                    placeholder="Фильтр по национальности"
                    value={nationalityFilter}
                    onChange={(e) => setNationalityFilter(e.target.value)}
                />
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

export default Persons;