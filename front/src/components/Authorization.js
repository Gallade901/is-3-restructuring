import React, { useState} from "react";
import { NavLink, useNavigate } from "react-router-dom";

const Authorization = () => {
    const [login, setLogin] = useState('');
    const [password, setPassword] = useState('');
    const [serverResponse, setServerResponse] = useState('');
    const navigate = useNavigate();


    const handleChange = (event) => {
        const { name, value } = event.target;
        if (name === 'login') setLogin(value);
        if (name === 'password') setPassword(value);
    }

    const handleSubmit = async (event) => {
        event.preventDefault();

        const userData = {
            login: login,
            password: password,
            role: "user"
        };
        try {
            const response = await fetch(`${process.env.REACT_APP_BASE_URL}/user/authorization`,{
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData),
                credentials: "include",
            });
            if (response.ok) {
                navigate("/persons");//flats
            } else if(response.status === 401) {
                setServerResponse("Неверный логин или пароль");
            }
            else {
                setServerResponse("Ошибка авторизации");
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Произошла ошибка при отправке запроса');
        }
    }


    return (
        <div className="authorization">
            <h2>Авторизация</h2>
            <div className="authorization-form">
                <form onSubmit={handleSubmit}>
                    Введите логин
                    <input placeholder="login" name="login"  value={login} onChange={handleChange} />
                    <br/>
                    Введите пароль
                    <input placeholder="password" name="password" type="password" value={password} onChange={handleChange} />
                    <br/>
                    <button type="submit">Войти</button>
                </form>
                <NavLink className="nav-link" to="registration">Регистрация</NavLink>
                <div className="error-msg">{serverResponse}</div>
            </div>
        </div>
    )
}


export default Authorization