import React, { useEffect, useState } from "react";
import HeaderApp from "./HeaderApp";
import {useNavigate} from "react-router-dom";
import axios from 'axios';

const ApplicationsAdmin = () => {
    const role = localStorage.getItem("role");
    const navigate = useNavigate();
    const [applications, setApplications] = useState([]);
    const fetchApplications = async () => {
        try {
            const response = await axios.get(process.env.REACT_APP_BASE_URL + '/user/applications', {
                withCredentials: true
            });
            setApplications(response.data);
        } catch (error) {
            console.error("Ошибка запроса заявок:", error);
        }
    };

    const answerApplication = async (login, flag) => {
        try {
            const response = await fetch(
                `${process.env.REACT_APP_BASE_URL}/user/answerApplication`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    credentials: "include",
                    body: JSON.stringify({ login, flag }),
                }
            );

            if (response.ok) {
                fetchApplications();
            } else {
                console.error("Ошибка при ответе на заявку:", response.statusText);
            }
        } catch (error) {
            console.error("Ошибка запроса на ответ заявки:", error);
        }
    };

    useEffect(() => {
        if (role !== "ADMIN") {
            navigate("/persons");
            alert("Доступ к заявкам есть только у админa");
        }
        fetchApplications();
    }, []);

    return (
        <div>
            <HeaderApp/>
            <h1>Заявки на регистрацию</h1>
            <br/>
            <div>
                {applications.length > 0 ? (
                    <ul>
                        {applications.map((app) => (
                            <li key={app.login} style={{marginBottom: "10px"}}>
                                <span style={{marginRight: "20px"}}>
                                    Логин: {app.login}
                                </span>
                                <button style={{marginRight: "10px"}}
                                        onClick={() => answerApplication(app.login, true)}>Одобрить
                                </button>
                                <button onClick={() => answerApplication(app.login, false)}>Отклонить</button>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p>Заявок нет</p>
                )}
            </div>
        </div>
    );

}


export default ApplicationsAdmin;
