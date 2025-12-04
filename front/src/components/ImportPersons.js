import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import HeaderApp from "./HeaderApp";
import '../css/ImportPersons.css';

const ImportPersons = () => {
    const [selectedFile, setSelectedFile] = useState(null);
    const [importResult, setImportResult] = useState('');
    const [isImporting, setIsImporting] = useState(false);
    const [importMethod, setImportMethod] = useState('file'); // 'file' или 'json'
    const [jsonData, setJsonData] = useState('');
    const navigate = useNavigate();
    const login = localStorage.getItem('login');

    const handleFileSelect = (event) => {
        const file = event.target.files[0];
        if (file) {
            if (file.type === 'application/json' || file.name.endsWith('.json')) {
                setSelectedFile(file);
                setImportResult('');
            } else {
                setImportResult('Пожалуйста, выберите JSON файл');
                setSelectedFile(null);
            }
        }
    };

    const handleJsonInputChange = (event) => {
        setJsonData(event.target.value);
        setImportResult('');
    };

    const validateJsonStructure = (jsonData) => {
        if (!Array.isArray(jsonData)) {
            throw new Error('JSON должен содержать массив объектов');
        }

        if (jsonData.length === 0) {
            throw new Error('JSON массив не должен быть пустым');
        }

        jsonData.forEach((person, index) => {
            // Проверка обязательных полей
            const requiredFields = ['name', 'coordinateX', 'coordinateY', 'locationY', 'hairColor', 'height', 'birthday'];
            requiredFields.forEach(field => {
                if (person[field] === undefined || person[field] === null) {
                    throw new Error(`Объект ${index + 1}: поле '${field}' обязательно`);
                }
            });

            // Проверка типов данных
            if (typeof person.name !== 'string' || person.name.trim() === '') {
                throw new Error(`Объект ${index + 1}: имя должно быть непустой строкой`);
            }

            if (typeof person.coordinateX !== 'number') {
                throw new Error(`Объект ${index + 1}: coordinateX должно быть числом`);
            }

            if (typeof person.coordinateY !== 'number' || person.coordinateY <= -792) {
                throw new Error(`Объект ${index + 1}: coordinateY должно быть числом больше -792`);
            }

            if (typeof person.locationY !== 'number') {
                throw new Error(`Объект ${index + 1}: locationY должно быть числом`);
            }

            if (typeof person.height !== 'number' || person.height <= 0) {
                throw new Error(`Объект ${index + 1}: рост должен быть числом больше 0`);
            }

            if (person.weight !== undefined && person.weight !== null &&
                (typeof person.weight !== 'number' || person.weight <= 0)) {
                throw new Error(`Объект ${index + 1}: вес должен быть числом больше 0 или null`);
            }

            // Проверка опциональных полей
            if (person.eyeColor && typeof person.eyeColor !== 'string') {
                throw new Error(`Объект ${index + 1}: eyeColor должно быть строкой`);
            }

            if (person.nationality && typeof person.nationality !== 'string') {
                throw new Error(`Объект ${index + 1}: nationality должно быть строкой`);
            }
        });
    };

    const handleFileImport = async () => {
        if (!selectedFile) {
            setImportResult('Пожалуйста, выберите файл для импорта');
            return;
        }

        setIsImporting(true);
        setImportResult('');

        try {
            const formData = new FormData();
            formData.append('file', selectedFile);
            formData.append('login', login);

            const response = await fetch(`${process.env.REACT_APP_BASE_URL}/person/import-file`, {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });

            if (response.ok) {
                const result = await response.text();
                setImportResult(`✅ ${result}`);
                // Перенаправление к истории импорта через 2 секунды
                setTimeout(() => {
                    navigate('/import-history');
                }, 2000);
            } else {
                const errorText = await response.text();
                setImportResult(`❌ Ошибка: ${errorText} проверьте имена персонажей и работу сервисов`);
            }
        } catch (error) {
            setImportResult(`❌ Ошибка: ${error.message}`);
        } finally {
            setIsImporting(false);
        }
    };

    const handleJsonImport = async () => {
        if (!jsonData.trim()) {
            setImportResult('Пожалуйста, введите JSON данные');
            return;
        }

        setIsImporting(true);
        setImportResult('');

        try {
            const parsedData = JSON.parse(jsonData);

            // Валидация структуры JSON
            validateJsonStructure(parsedData);

            // Отправка на сервер через старый endpoint
            const response = await fetch(`${process.env.REACT_APP_BASE_URL}/person/import?login=${login}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(parsedData),
            });

            if (response.ok) {
                const result = await response.text();
                setImportResult(`✅ ${result}`);
                // Перенаправление к списку persons через 2 секунды
                setTimeout(() => {
                    navigate('/persons');
                }, 2000);
            } else {
                const errorText = await response.text();
                setImportResult(`❌ Ошибка: ${errorText}`);
            }
        } catch (error) {
            setImportResult(`❌ Ошибка: ${error.message}`);
        } finally {
            setIsImporting(false);
        }
    };

    const handleImport = () => {
        if (importMethod === 'file') {
            handleFileImport();
        } else {
            handleJsonImport();
        }
    };

    const readFileContent = (file) => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (event) => resolve(event.target.result);
            reader.onerror = (error) => reject(error);
            reader.readAsText(file);
        });
    };

    const loadSampleData = () => {
        const sampleData = [
            {
                "name": "Иван Иванов",
                "coordinateX": 123.45,
                "coordinateY": 67.89,
                "locationX": 10.5,
                "locationY": 20.3,
                "locationZ": 5.7,
                "eyeColor": "BLUE",
                "hairColor": "BLACK",
                "height": 180,
                "birthday": "1990-05-15",
                "weight": 75.5,
                "nationality": "RUSSIA"
            },
            {
                "name": "Мария Петрова",
                "coordinateX": 111.22,
                "coordinateY": 33.44,
                "locationX": 15.2,
                "locationY": 25.8,
                "locationZ": 8.1,
                "eyeColor": "GREEN",
                "hairColor": "BROWN",
                "height": 165,
                "birthday": "1985-08-20",
                "weight": 60.0,
                "nationality": "GERMANY"
            }
        ];
        setJsonData(JSON.stringify(sampleData, null, 2));
        setImportResult('');
    };

    const clearJsonData = () => {
        setJsonData('');
        setImportResult('');
    };

    const isImportDisabled = () => {
        if (importMethod === 'file') {
            return !selectedFile || isImporting;
        } else {
            return !jsonData.trim() || isImporting;
        }
    };

    return (
        <div className="import-persons-container">
            <HeaderApp />
            <div className="import-persons-content">
                <h1>Импорт персонажей</h1>

                <div className="import-method-selector">
                    <label>
                        <input
                            type="radio"
                            value="file"
                            checked={importMethod === 'file'}
                            onChange={(e) => setImportMethod(e.target.value)}
                        />
                        📁 Импорт из файла (с сохранением в MinIO)
                    </label>
                    <label>
                        <input
                            type="radio"
                            value="json"
                            checked={importMethod === 'json'}
                            onChange={(e) => setImportMethod(e.target.value)}
                        />
                        📝 Импорт из JSON текста
                    </label>
                </div>

                <div className="import-info">

                    {importMethod === 'file' && (
                        <div className="file-info">

                        </div>
                    )}
                </div>

                {importMethod === 'file' ? (
                    <div className="import-controls">
                        <div className="file-input-container">
                            <input
                                type="file"
                                accept=".json,application/json"
                                onChange={handleFileSelect}
                                className="file-input"
                                id="file-input"
                            />
                            <label htmlFor="file-input" className="file-input-label">
                                {selectedFile ? `📄 ${selectedFile.name}` : '📁 Выберите JSON файл'}
                            </label>
                            {selectedFile && (
                                <div className="file-info">
                                    Размер: {(selectedFile.size / 1024).toFixed(2)} KB
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    <div className="json-input-container">
                        <div className="json-input-header">
                            <h4>JSON данные:</h4>
                            <div className="json-buttons">
                                <button
                                    type="button"
                                    onClick={loadSampleData}
                                    className="sample-button"
                                >
                                    📋 Загрузить пример
                                </button>
                                <button
                                    type="button"
                                    onClick={clearJsonData}
                                    className="clear-button"
                                >
                                    🗑️ Очистить
                                </button>
                            </div>
                        </div>
                        <textarea
                            value={jsonData}
                            onChange={handleJsonInputChange}
                            placeholder={`Введите JSON массив, например:\n[\n  {\n    "name": "Имя",\n    "coordinateX": 100,\n    "coordinateY": 200,\n    "locationY": 50,\n    "hairColor": "BLACK",\n    "height": 180,\n    "birthday": "1990-01-01"\n  }\n]`}
                            className="json-textarea"
                            rows={15}
                        />
                        {jsonData && (
                            <div className="json-stats">
                                Объектов: {(() => {
                                try {
                                    const data = JSON.parse(jsonData);
                                    return Array.isArray(data) ? data.length : 0;
                                } catch {
                                    return 0;
                                }
                            })()}
                            </div>
                        )}
                    </div>
                )}

                <div className="import-actions">
                    <button
                        onClick={handleImport}
                        disabled={isImportDisabled()}
                        className="import-button"
                    >
                        {isImporting ? '⏳ Импорт...' : '📤 Импортировать'}
                    </button>

                    <button
                        onClick={() => navigate('/import-history')}
                        className="history-button"
                    >
                        📊 История импорта
                    </button>
                </div>

                {importResult && (
                    <div className={`import-result ${importResult.includes('✅') ? 'success' : 'error'}`}>
                        {importResult}
                    </div>
                )}

                {/* Подсказка по формату JSON */}
                <div className="json-format-help">

                </div>
            </div>
        </div>
    );
};

export default ImportPersons;