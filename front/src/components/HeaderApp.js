import React from "react";
import {useLocation, useNavigate} from "react-router-dom";

const HeaderApp = () => {
    const user = localStorage.getItem("login");
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = async () => {
        try {
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/user/logout`,
                {
                    method: "POST",
                    credentials: "include",
                }
            );

            if (response.ok) {
                localStorage.removeItem("user");
                window.location.reload();
                navigate("/");
            } else {
                console.error("Ошибка выхода:", response.status);
            }
        } catch (error) {
            console.error("Ошибка запроса выхода:", error);
        }
    };

    const handleSelectChange = (event) => {
        const selectedPath = event.target.value;
        if (selectedPath) {
            navigate(selectedPath);
        }
    };

    return (
        <header className="header">
            <div className="left">
                <select onChange={handleSelectChange} value={location.pathname}>
                    <option value="/persons">Люди</option>
                    <option value="/addPerson">Добавить человека</option>
                    <option value="/applications">Заявки</option>
                    <option value="/addCoordinate">Добавить координаты</option>
                    <option value="/addLocation">Добавить расположение</option>
                    <option value="/locations">Расположения</option>
                    <option value="/coordinates">Координаты</option>
                    <option value="/functions">Функции</option>
                    <option value="/import-persons">Импорт файла</option>
                    <option value="/import-history">История имопрта</option>
                </select>
            </div>

            <div className="right">
                User: {user}
                {user && <button onClick={handleLogout}>Выйти</button>}
            </div>
        </header>
    );
};

export default HeaderApp;
