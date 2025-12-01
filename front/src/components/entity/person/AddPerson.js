import React, { useState, useEffect } from "react";
import HeaderApp from "../../HeaderApp";

const AddPerson = () => {
    const login = localStorage.getItem("login");
    const [formData, setFormData] = useState({
        name: "",
        coordinatesId: 0,
        coordinateX: "",
        coordinateY: "",
        locationId: 0,
        locationX: "",
        locationY: "",
        locationZ: "",
        eyeColor: null,
        hairColor: null,
        height: "",
        birthday: "",
        weight: "",
        nationality: null,
        login: login,
    });

    const [coordinatesData, setCoordinatesData] = useState();
    const [locationsData, setLocationsData] = useState();
    const [errors, setErrors] = useState({});
    const [serverResponse, setServerResponse] = useState("");

    useEffect(() => {
        // Загрузка координат
        fetch(`${process.env.REACT_APP_BASE_URL}/coordinates`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: "include",
        })
            .then((response) => response.json())
            .then((data) => {
                setCoordinatesData(data);
            })
            .catch((error) => console.error("Ошибка при загрузке координат:", error));

        // Загрузка локаций
        fetch(`${process.env.REACT_APP_BASE_URL}/location`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: "include",
        })
            .then((response) => response.json())
            .then((data) => {
                setLocationsData(data);
            })
            .catch((error) => console.error("Ошибка при загрузке локаций:", error));
    }, []);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({
            ...formData,
            [name]: type === "checkbox" ? checked : value,
        });
    };

    const validateForm = () => {
        const validationErrors = {};

        if (!formData.name.trim())
            validationErrors.name = "Имя не может быть пустым";

        if (formData.coordinateX === "" || formData.coordinateX === null) {
            validationErrors.coordinateX = "Координата X не может быть пустой";
        }

        if (formData.coordinateY === "" || formData.coordinateY === null || formData.coordinateY <= -792) {
            validationErrors.coordinateY = "Координата Y должна быть больше -792";
        }

        if (formData.locationX === "" || formData.locationX === null) {
            validationErrors.locationX = "Локация X не может быть пустой";
        }

        if (!formData.hairColor) {
            validationErrors.hairColor = "Цвет волос должен быть выбран";
        }

        if (!formData.height || formData.height <= 0) {
            validationErrors.height = "Рост должен быть больше 0";
        }

        if (!formData.birthday) {
            validationErrors.birthday = "День рождения должен быть указан";
        }

        if (formData.weight && formData.weight <= 0) {
            validationErrors.weight = "Вес должен быть больше 0";
        }

        setErrors(validationErrors);
        return Object.keys(validationErrors).length === 0;
    };

    const [isNewCoordinate, setIsNewCoordinate] = useState(true);
    const [isNewLocation, setIsNewLocation] = useState(true);

    const handleCoordinateChange = (e) => {
        const selectedCoordinateId = e.target.value;
        if (selectedCoordinateId === "0") {
            setIsNewCoordinate(true);
            setFormData({
                ...formData,
                coordinatesId: 0,
                coordinateX: "",
                coordinateY: "",
            });
        } else {
            let selectedCoordinate;
            if (Array.isArray(coordinatesData)) {
                selectedCoordinate = coordinatesData.find(
                    (coordinates) => coordinates.id === parseInt(selectedCoordinateId)
                );
            }
            setFormData({
                ...formData,
                coordinatesId: selectedCoordinate?.id,
                coordinateX: selectedCoordinate?.x,
                coordinateY: selectedCoordinate?.y,
            });
            setIsNewCoordinate(false);
        }
    };

    const handleLocationChange = (e) => {
        const selectedLocationId = e.target.value;
        if (selectedLocationId === "0") {
            setIsNewLocation(true);
            setFormData({
                ...formData,
                locationId: 0,
                locationX: "",
                locationY: "",
                locationZ: "",
            });
        } else {
            let selectedLocation;
            if (Array.isArray(locationsData)) {
                selectedLocation = locationsData.find(
                    (location) => location.id === parseInt(selectedLocationId)
                );
            }
            setFormData({
                ...formData,
                locationId: selectedLocation?.id,
                locationX: selectedLocation?.x,
                locationY: selectedLocation?.y,
                locationZ: selectedLocation?.z,
            });
            setIsNewLocation(false);
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!validateForm()) {
            setServerResponse("");
            return;
        }

        try {
            const response = await fetch(`${process.env.REACT_APP_BASE_URL}/person`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(formData),
                credentials: "include",
            });

            if (response.ok) {
                const result = await response.text();
                setServerResponse(result);
            } else {
                setServerResponse("Ошибка отправки проверти имя");
            }
        } catch (error) {
            console.error("Ошибка:", error);
            alert("Произошла ошибка при отправке запроса");
        }
    };

    return (
        <div>
            <HeaderApp />
            <div className="add-component">
                <form className="form-add" onSubmit={handleSubmit}>
                    <input
                        name="name"
                        placeholder="Имя"
                        value={formData.name}
                        onChange={handleChange}
                    />
                    {errors.name && <div className="error-field">{errors.name}</div>}

                    <h3>Координаты</h3>
                    <select
                        name="coordinatesId"
                        value={formData.coordinatesId}
                        onChange={handleCoordinateChange}
                    >
                        <option value="0">Создать координаты</option>
                        {coordinatesData?.map((coordinates) => (
                            <option key={coordinates.id} value={coordinates.id}>
                                {coordinates.id}-({coordinates.x}, {coordinates.y})
                            </option>
                        ))}
                    </select>
                    <input
                        name="coordinateX"
                        type="number"
                        step="any"
                        placeholder="Координата X"
                        value={formData.coordinateX}
                        onChange={handleChange}
                        disabled={!isNewCoordinate}
                    />
                    {errors.coordinateX && <div className="error-field">{errors.coordinateX}</div>}
                    <input
                        name="coordinateY"
                        type="number"
                        step="any"
                        placeholder="Координата Y"
                        value={formData.coordinateY}
                        onChange={handleChange}
                        disabled={!isNewCoordinate}
                    />
                    {errors.coordinateY && <div className="error-field">{errors.coordinateY}</div>}

                    <h3>Локация</h3>
                    <select
                        name="locationId"
                        value={formData.locationId}
                        onChange={handleLocationChange}
                    >
                        <option value="0">Создать локацию</option>
                        {locationsData?.map((location) => (
                            <option key={location.id} value={location.id}>
                                {location.id}-({location.x}, {location.y}, {location.z})
                            </option>
                        ))}
                    </select>
                    <input
                        name="locationX"
                        type="number"
                        step="any"
                        placeholder="Локация X"
                        value={formData.locationX}
                        onChange={handleChange}
                        disabled={!isNewLocation}
                    />
                    {errors.locationX && <div className="error-field">{errors.locationX}</div>}
                    <input
                        name="locationY"
                        type="number"
                        step="any"
                        placeholder="Локация Y"
                        value={formData.locationY}
                        onChange={handleChange}
                        disabled={!isNewLocation}
                    />
                    <input
                        name="locationZ"
                        type="number"
                        placeholder="Локация Z"
                        value={formData.locationZ}
                        onChange={handleChange}
                        disabled={!isNewLocation}
                    />

                    <h3>Внешность</h3>
                    <select
                        name="eyeColor"
                        value={formData.eyeColor || ""}
                        onChange={handleChange}
                    >
                        <option value="">Цвет глаз (необязательно)</option>
                        <option value="GREEN">Зеленый</option>
                        <option value="YELLOW">Желтый</option>
                        <option value="ORANGE">Оранжевый</option>
                        <option value="BROWN">Коричневый</option>
                    </select>

                    <select
                        name="hairColor"
                        value={formData.hairColor || ""}
                        onChange={handleChange}
                    >
                        <option value="">Цвет волос</option>
                        <option value="GREEN">Зеленый</option>
                        <option value="YELLOW">Желтый</option>
                        <option value="ORANGE">Оранжевый</option>
                        <option value="BROWN">Коричневый</option>
                    </select>
                    {errors.hairColor && <div className="error-field">{errors.hairColor}</div>}

                    <h3>Физические параметры</h3>
                    <input
                        name="height"
                        type="number"
                        placeholder="Рост"
                        value={formData.height}
                        onChange={handleChange}
                    />
                    {errors.height && <div className="error-field">{errors.height}</div>}

                    <input
                        name="weight"
                        type="number"
                        placeholder="Вес (необязательно)"
                        value={formData.weight}
                        onChange={handleChange}
                    />
                    {errors.weight && <div className="error-field">{errors.weight}</div>}

                    <h3>Другое</h3>
                    <input
                        name="birthday"
                        type="datetime-local"
                        placeholder="День рождения"
                        value={formData.birthday}
                        onChange={handleChange}
                    />
                    {errors.birthday && <div className="error-field">{errors.birthday}</div>}

                    <select
                        name="nationality"
                        value={formData.nationality || ""}
                        onChange={handleChange}
                    >
                        <option value="">Национальность (необязательно)</option>
                        <option value="UNITED_KINGDOM">Великобритания</option>
                        <option value="VATICAN">Ватикан</option>
                        <option value="NORTH_KOREA">Северная Корея</option>
                    </select>

                    <button type="submit">Добавить</button>
                    <div>{serverResponse}</div>
                </form>
            </div>
        </div>
    );
};

export default AddPerson;