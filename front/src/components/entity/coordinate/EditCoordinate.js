import React, {useEffect, useState} from "react";
import HeaderApp from "../../HeaderApp";
import {useNavigate, useParams} from "react-router-dom";

const EditCoordinate = () => {
    const login = localStorage.getItem("login");
    const { id } = useParams();
    const navigate = useNavigate();
    const role = localStorage.getItem("role");
    const [formData, setFormData] = useState({
        id: 0,
        x: 0.0,
        y: 0.0,
    });

    const [serverResponse, setServerResponse] = useState('');
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchCoordinate = async () => {
            try {
                const response = await fetch(
                    `${process.env.REACT_APP_BASE_URL}/coordinates/getId?id=${id}`,
                    {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        credentials: "include",
                    }
                );

                if (!response.ok) {
                    navigate("/coordinates");
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                if (!data) {
                    throw new Error('Пустой ответ от сервера');
                }

                if (data.owner !== login && role !== "ADMIN") {
                    navigate("/coordinates");
                    return;
                }

                setFormData({
                    ...data,
                    x: data.x || 0.0,
                    y: data.y || 0.0
                });
            } catch (error) {
                console.error("Ошибка при загрузке данных:", error);
                navigate("/coordinates");
            } finally {
                setIsLoading(false);
            }
        };

        fetchCoordinate();
    }, [id, login, role, navigate]);

    const [errors, setErrors] = useState({
        x: false,
        y: false,
    });

    const handleChange = (e) => {
        const { name, value } = e.target;

        const numValue = parseFloat(value);
        if (isNaN(numValue)) {
            return;
        }

        let validatedValue = numValue;
        if (name === "y" && numValue <= -792) {
            validatedValue = -791;
        }

        setFormData({
            ...formData,
            [name]: validatedValue,
        });
        setErrors({
            ...errors,
            [name]: false,
        });
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        const validationErrors = {
            x: formData.x === null || formData.x === undefined || isNaN(formData.x),
            y: formData.y === null || formData.y === undefined || isNaN(formData.y) || formData.y <= -792,
        };

        if (validationErrors.x || validationErrors.y) {
            setErrors(validationErrors);
            return;
        }

        try {
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/coordinates`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData),
                    credentials: "include",
                }
            );

            if (response.ok) {
                const result = await response.text();
                setServerResponse(result);
                setTimeout(() => {
                    navigate("/coordinates");
                }, 1000);
            } else {
                const errorText = await response.text();
                setServerResponse(`Ошибка отправки: ${errorText}`);
            }
        } catch (error) {
            console.error('Ошибка:', error);
            setServerResponse('Произошла ошибка при отправке запроса');
        }
    };

    if (isLoading) {
        return (
            <div>
                <HeaderApp />
                <div className="add-component">Загрузка...</div>
            </div>
        );
    }

    return (
        <div>
            <HeaderApp />
            <div className="add-component">
                <form className="form-add" onSubmit={handleSubmit}>
                    <input
                        type="number"
                        step="0.01"
                        name="x"
                        value={formData.x}
                        placeholder="Coordinate-x"
                        onChange={handleChange}
                    />
                    {errors.x && <div className="error-field">Поле X не может быть пустым</div>}

                    <input
                        type="number"
                        step="0.01"
                        name="y"
                        value={formData.y}
                        placeholder="Coordinate-y (min -791)"
                        onChange={handleChange}
                    />
                    {errors.y && <div className="error-field">Поле Y должно быть больше -792</div>}

                    <button type="submit">Изменить</button>
                    {serverResponse && <div className="server-response">{serverResponse}</div>}
                </form>
            </div>
        </div>
    );
};

export default EditCoordinate;