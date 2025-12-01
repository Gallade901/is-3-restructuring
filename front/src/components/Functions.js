import { useState } from "react";
import axios from 'axios';
import HeaderApp from "./HeaderApp";

const Functions = () => {
    const [height, setHeight] = useState('');
    const [hairColor, setHairColor] = useState('');
    const [result, setResult] = useState(null);
    const [message, setMessage] = useState('');
    const [nationalityData, setNationalityData] = useState([]);

    function clearResults() {
        setResult(null);
        setMessage('');
        setNationalityData([]);
    }

    const handleAverageHeight = async (event) => {
        event.preventDefault();
        clearResults();
        try {
            const { data } = await axios.get(`${process.env.REACT_APP_BASE_URL}/person-functions/average-height`, {
                withCredentials: true
            });
            setResult(`Средний рост: ${data.toFixed(2)}`);
        } catch (error) {
            setMessage(`Проверьте наличие людей`);
        }
    };

    const handleCountByNationality = async (event) => {
        event.preventDefault();
        clearResults();
        try {
            const { data } = await axios.get(`${process.env.REACT_APP_BASE_URL}/person-functions/count-by-nationality`, {
                withCredentials: true
            });
            if (data.length === 0) {
                setMessage("Проверьте наличие людей")
            } else {
                setNationalityData(data)
            }
        } catch (error) {
            setMessage(`Ошибка: ${error.response?.data || error.message}`);
        }
    };

    const handleCountHeightGreater = async (event) => {
        event.preventDefault();
        clearResults();
        try {
            const { data } = await axios.get(`${process.env.REACT_APP_BASE_URL}/person-functions/count-height-greater`, {
                params: { height: height },
                withCredentials: true
            });
            setResult(`Количество людей с ростом больше ${height}: ${data}`);
        } catch (error) {
            setMessage(`Ошибка: ${error.response?.data || error.message}`);
        }
    };

    const handleCountByHairColor = async (event) => {
        event.preventDefault();
        clearResults();
        try {
            const { data } = await axios.get(`${process.env.REACT_APP_BASE_URL}/person-functions/count-by-hair-color`, {
                params: { hairColor: hairColor },
                withCredentials: true
            });
            setResult(`Количество людей с цветом волос ${hairColor}: ${data}`);
        } catch (error) {
            setMessage(`Ошибка: ${error.response?.data || error.message}`);
        }
    };

    const handleHairColorPercentage = async (event) => {
        event.preventDefault();
        clearResults();
        try {
            const { data } = await axios.get(`${process.env.REACT_APP_BASE_URL}/person-functions/hair-color-percentage`, {
                params: { hairColor: hairColor },
                withCredentials: true
            });
            setResult(`Доля людей с цветом волос ${hairColor}: ${data.toFixed(2)}%`);
        } catch (error) {
            setMessage(`Ошибка: ${error.response?.data || error.message}`);
        }
    };

    return (
        <div>
            <HeaderApp />
            <div className="full">
                <div className="function-section">
                    <form onSubmit={handleAverageHeight}>
                        <button type="submit">Рассчитать средний рост</button>
                    </form>
                </div>

                <div className="function-section">
                    <form onSubmit={handleCountByNationality}>
                        <button type="submit">Статистика по национальностям</button>
                    </form>
                </div>

                <div className="function-section">
                    <form onSubmit={handleCountHeightGreater}>
                        <label htmlFor="height">Минимальный рост:</label>
                        <input
                            id="height"
                            type="number"
                            value={height}
                            onChange={(e) => setHeight(e.target.value)}
                            min="1"
                        />
                        <button type="submit" disabled={!height.trim()}>Количество людей с ростом больше</button>
                    </form>
                </div>

                <div className="function-section">
                    <form onSubmit={handleCountByHairColor}>
                        <label htmlFor="hairColorCount">Цвет волос:</label>
                        <select
                            id="hairColorCount"
                            value={hairColor}
                            onChange={(e) => setHairColor(e.target.value)}
                        >
                            <option value="GREEN">Зеленый</option>
                            <option value="YELLOW">Желтый</option>
                            <option value="ORANGE">Оранжевый</option>
                            <option value="BROWN">Коричневый</option>
                        </select>
                        <button type="submit">Количество по цвету волос</button>
                    </form>
                </div>

                <div className="function-section">
                    <form onSubmit={handleHairColorPercentage}>
                        <label htmlFor="hairColorPercentage">Цвет волос:</label>
                        <select
                            id="hairColorPercentage"
                            value={hairColor}
                            onChange={(e) => setHairColor(e.target.value)}
                        >
                            <option value="GREEN">Зеленый</option>
                            <option value="YELLOW">Желтый</option>
                            <option value="ORANGE">Оранжевый</option>
                            <option value="BROWN">Коричневый</option>
                        </select>
                        <button type="submit">Доля по цвету волос (%)</button>
                    </form>
                </div>

                {message && <div className="message error">{message}</div>}
                {result && <div className="message success">{result}</div>}

                {nationalityData.length > 0 && (
                    <div className="data-section">
                        <h3>Статистика по национальностям:</h3>
                        <table>
                            <thead>
                            <tr>
                                <th>Национальность</th>
                                <th>Количество</th>
                            </tr>
                            </thead>
                            <tbody>
                            {nationalityData.map((item, index) => (
                                <tr key={index}>
                                    <td>{item.nationality || 'Не указано'}</td>
                                    <td>{item.count}</td>
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

export default Functions;