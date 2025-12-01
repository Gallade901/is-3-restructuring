import React, { useState } from "react";
import HeaderApp from "../../HeaderApp";

const AddCoordinate = () => {
    const login = localStorage.getItem("login")
    const [formData, setFormData] = useState({
        x: 0.0,
        y: 0.0,
        login: login,
    });
    const [serverResponse, setServerResponse] = useState('');
    const [errors, setErrors] = useState({
        x: false,
        y: false,
    });

    const handleChange = (e) => {
        const { name, value } = e.target;

        // Валидация для Double чисел
        const numValue = parseFloat(value);
        if (isNaN(numValue)) {
            return;
        }

        // Проверка ограничений
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
                    method: 'POST',
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
                // Очистка формы после успешной отправки
                setFormData({
                    x: 0.0,
                    y: 0.0,
                    login: login,
                });
            } else {
                const errorText = await response.text();
                setServerResponse(`Ошибка отправки: ${errorText}`);
            }
        } catch (error) {
            console.error('Ошибка:', error);
            setServerResponse('Произошла ошибка при отправке запроса');
        }
    };

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

                    <button type="submit">Добавить</button>
                    {serverResponse && <div className="server-response">{serverResponse}</div>}
                </form>
            </div>
        </div>
    );
}

export default AddCoordinate;