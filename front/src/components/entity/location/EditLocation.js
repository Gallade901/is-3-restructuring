import React, {useEffect, useState} from "react";
import HeaderApp from "../../HeaderApp";
import {useNavigate, useParams} from "react-router-dom";

const EditLocation = () => {
    const login = localStorage.getItem("login");
    const { id } = useParams();
    const navigate = useNavigate();
    const role = localStorage.getItem("role");
    const [formData, setFormData] = useState({
        id: 0,
        x: 0.0,
        y: 0.0,
        z: 0,
    });

    const [serverResponse, setServerResponse] = useState('');
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchLocation = async () => {
            try {
                const response = await fetch(
                    `${process.env.REACT_APP_BASE_URL}/location/getId?id=${id}`,
                    {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        credentials: "include",
                    }
                );

                if (!response.ok) {
                    navigate("/locations");
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                if (!data) {
                    throw new Error('Пустой ответ от сервера');
                }

                if (data.owner !== login && role !== "ADMIN") {
                    navigate("/locations");
                    return;
                }

                setFormData({
                    ...data,
                    x: data.x || 0.0,
                    y: data.y || 0.0,
                    z: data.z || 0
                });
            } catch (error) {
                console.error("Ошибка при загрузке данных:", error);
                navigate("/locations");
            } finally {
                setIsLoading(false);
            }
        };

        fetchLocation();
    }, [id, login, role, navigate]);

    const [errors, setErrors] = useState({
        x: false,
        y: false,
        z: false,
    });

    const handleChange = (e) => {
        const { name, value } = e.target;

        if (name === "x" || name === "y") {
            const numValue = parseFloat(value);
            if (isNaN(numValue)) {
                return;
            }
            setFormData({
                ...formData,
                [name]: numValue,
            });
        } else if (name === "z") {
            const intValue = parseInt(value);
            if (isNaN(intValue)) {
                return;
            }
            setFormData({
                ...formData,
                [name]: intValue,
            });
        }

        setErrors({
            ...errors,
            [name]: false,
        });
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        const validationErrors = {
            x: formData.x === null || formData.x === undefined || isNaN(formData.x),
            y: formData.y === null || formData.y === undefined || isNaN(formData.y),
            z: formData.z === null || formData.z === undefined || isNaN(formData.z),
        };

        if (validationErrors.x || validationErrors.y || validationErrors.z) {
            setErrors(validationErrors);
            return;
        }

        try {
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/location`,
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
                    navigate("/locations");
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
                        placeholder="Coordinate-x (Float)"
                        onChange={handleChange}
                    />
                    {errors.x && <div className="error-field">Поле X не может быть пустым</div>}

                    <input
                        type="number"
                        step="0.01"
                        name="y"
                        value={formData.y}
                        placeholder="Coordinate-y (double)"
                        onChange={handleChange}
                    />
                    {errors.y && <div className="error-field">Поле Y не может быть пустым</div>}

                    <input
                        type="number"
                        name="z"
                        value={formData.z}
                        placeholder="Coordinate-z (int)"
                        onChange={handleChange}
                    />
                    {errors.z && <div className="error-field">Поле Z не может быть пустым</div>}

                    <button type="submit">Изменить</button>
                    {serverResponse && <div className="server-response">{serverResponse}</div>}
                </form>
            </div>
        </div>
    );
};

export default EditLocation;