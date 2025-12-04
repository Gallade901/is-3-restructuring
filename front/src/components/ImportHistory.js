// ImportHistory.js
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import HeaderApp from "./HeaderApp";
import '../css/ImportHistory.css';

const ImportHistory = () => {
    const [importHistory, setImportHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const login = localStorage.getItem('login');
    const role = localStorage.getItem('role');

    useEffect(() => {
        fetchImportHistory();
    }, []);

    const fetchImportHistory = async () => {
        try {
            setLoading(true);
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/person/import-history?login=${login}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                }
            );

            if (response.ok) {
                const data = await response.json();
                setImportHistory(data);
            } else {
                setError('Ошибка при загрузке истории импорта');
            }
        } catch (error) {
            setError('Ошибка при загрузке истории импорта');
        } finally {
            setLoading(false);
        }
    };

    const handleDownload = async (historyId, fileName) => {
        try {
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/person/import-history/${historyId}/file`,
                {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                }
            );

            if (response.ok) {
                const data = await response.json();

                // Создаем временную ссылку для скачивания
                const link = document.createElement('a');
                link.href = data.url;
                link.download = fileName || 'download';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

            } else {
                alert('Ошибка при скачивании файла');
            }
        } catch (error) {
            console.error('Download error:', error);
            alert('Ошибка при скачивании файла');
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString('ru-RU');
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case 'SUCCESS':
                return <span className="status-badge success">Успешно</span>;
            case 'ERROR':
                return <span className="status-badge error">Ошибка</span>;
            case 'IN_PROGRESS':
                return <span className="status-badge in-progress">В процессе</span>;
            default:
                return <span className="status-badge unknown">{status}</span>;
        }
    };

    if (loading) {
        return (
            <div className="import-history-container">
                <HeaderApp />
                <div className="loading">Загрузка истории импорта...</div>
            </div>
        );
    }

    return (
        <div className="import-history-container">
            <HeaderApp />
            <div className="import-history-content">
                <div className="import-history-header">
                    <h1>История импорта</h1>
                    <button
                        onClick={() => navigate('/import-persons')}
                        className="back-to-import-button"
                    >
                        📤 Новый импорт
                    </button>
                </div>

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                {importHistory.length === 0 ? (
                    <div className="no-history">
                        История импорта пуста
                    </div>
                ) : (
                    <div className="history-table-container">
                        <table className="history-table">
                            <thead>
                            <tr>
                                <th>ID операции</th>
                                <th>Пользователь</th>
                                <th>Статус</th>
                                <th>Количество объектов</th>
                                <th>Дата и время</th>
                                <th>Файл</th>
                            </tr>
                            </thead>
                            <tbody>
                            {importHistory.map((history) => (
                                <tr key={history.id}>
                                    <td className="history-id">#{history.id}</td>
                                    <td className="user-login">{history.userLogin}</td>
                                    <td className="status-cell">
                                        {getStatusBadge(history.status)}
                                    </td>
                                    <td className="imported-count">
                                        {history.status === 'SUCCESS'
                                            ? history.importedCount
                                            : '-'}
                                    </td>
                                    <td className="import-date">
                                        {formatDate(history.importDate)}
                                    </td>
                                    <td className="file-cell">
                                        {history.fileStored && history.fileName && (
                                            <button
                                                onClick={() => handleDownload(history.id, history.fileName)}
                                                className="download-button"
                                                title="Скачать исходный файл"
                                            >
                                                📎 Скачать
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ImportHistory;