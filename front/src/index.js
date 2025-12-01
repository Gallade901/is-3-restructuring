import React from "react";
import * as ReactDOMClient from "react-dom/client";
import App from "./App";
import "./css/main.css"

// Создаем контейнер
const root = ReactDOMClient.createRoot(document.getElementById('root'));

// Рендерим элементы
root.render(<App/>);