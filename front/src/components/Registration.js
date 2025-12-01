import React from "react";
import {NavLink} from "react-router-dom";

class Registration extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            login: '',
            password: '',
            role: 'USER',
            serverResponse: '',
            errorMessage: '',
        };
    }
    handleInputChange = (event) => {
        const { name, value } = event.target;
        this.setState({ [name]: value, errorMessage: '' });
    }
    validateInputs = () => {
        const { login, password } = this.state;
        if (login.length < 4 || password.length < 4 || login.length > 20 || password.length > 20) {
            this.setState({ errorMessage: 'Логин и пароль должны содержать не менее 4 и не более 20 символов.' });
            return false;
        }
        return true;
    }
    handleSubmit = async (event) => {
        event.preventDefault();
        if (!this.validateInputs()) {
            return;
        }

        const userData = {
            login: this.state.login,
            password: this.state.password,
            role: this.state.role
        };

        try {
            const response = await fetch(`${process.env.REACT_APP_BASE_URL}/user/registration`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });
            if (response.ok) {
                const result = await response.text();
                this.setState({ serverResponse: result});
            } else {
                this.setState({ serverResponse: "Ошибка регистрации"});
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Произошла ошибка при отправке запроса');
        }
    }

    render () {
        return (
            <div className="authorization">
                <h2>Регистрация</h2>
                <div className="authorization-form">
                    <form onSubmit={this.handleSubmit}>
                        Введите логин
                        <input
                            name="login"
                            placeholder="login"
                            value={this.state.login}
                            onChange={this.handleInputChange}
                        />
                        <br/>
                        Введите пароль
                        <input
                            name="password"
                            type="password"
                            placeholder="password"
                            value={this.state.password}
                            onChange={this.handleInputChange}
                        />
                        <br/>
                        <label htmlFor="role">Тип пользователя</label>
                        <select id="role" name="role" value={this.state.role} onChange={this.handleInputChange}>
                            <option value="USER">User</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                        <button type="submit">Зарегистрироваться</button>
                    </form>
                    <NavLink className="nav-link" to="/">У меня уже есть аккаунт</NavLink>
                    <br/>
                    <div className="response-msg">
                        <span>{this.state.serverResponse}</span>
                    </div>
                    <div className="error-msg">{this.state.errorMessage}</div>
                </div>
            </div>
        )
    }
}

export default Registration