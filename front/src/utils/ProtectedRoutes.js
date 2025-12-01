import React, { useEffect, useState } from "react";
import { Navigate, Outlet } from "react-router-dom";

const ProtectedRoutes = () => {
    const [isAuthenticated, setIsAuthenticated] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkSession = async () => {
            try {
                const response = await fetch(
                    `${process.env.REACT_APP_BASE_URL}/user/checkAuthorization`,
                    {
                        method: "GET",
                        credentials: "include",
                    }
                );

                if (response.ok) {
                    const data = await response.json();
                    const role = data.role;
                    const login = data.login;
                    setIsAuthenticated(true)
                    localStorage.setItem("login", login);
                    localStorage.setItem("role", role);
                } else {
                    setIsAuthenticated(false);
                }
            } catch (error) {
                console.error("Ошибка проверки авторизации:", error);
                setIsAuthenticated(false);
            } finally {
                setLoading(false);
            }
        };

        checkSession();
    }, []);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
};

export default ProtectedRoutes;
