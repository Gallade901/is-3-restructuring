import "./css/noAutoriz.css"
import {
    createHashRouter,
    createRoutesFromElements,
    RouterProvider,
    Route,
    useLocation
} from "react-router-dom";
import Authorization from "./components/Authorization";
import Registration from "./components/Registration";
import ProtectedRoutes from "./utils/ProtectedRoutes";
import PublicRoutes from "./utils/PublicRoutes";
import ApplicationsAdmin from "./components/ApplicationsAdmin";
import AddCoordinate from "./components/entity/coordinate/AddCoordinate";
import Coordinates from "./components/entity/coordinate/Coordinates";
import EditCoordinate from "./components/entity/coordinate/EditCoordinate";
import Functions from "./components/Functions";
import Persons from "./components/entity/person/Persons";
import EditPerson from "./components/entity/person/EditPerson";
import AddPerson from "./components/entity/person/AddPerson";
import EditLocation from "./components/entity/location/EditLocation";
import AddLocation from "./components/entity/location/AddLocation";
import Locations from "./components/entity/location/Locations";
import ImportPersons from "./components/ImportPersons";
import ImportHistory from "./components/ImportHistory";

const ProtectedRoutesWrapper = () => {
    const location = useLocation();
    return <ProtectedRoutes key={location.pathname} />;
};

// Убираем basename для HashRouter, так как он работает по-другому
const router = createHashRouter(
    createRoutesFromElements(
        <Route path="/">
            <Route element={<PublicRoutes />}>
                <Route index element={<Authorization />} />
                <Route path="registration" element={<Registration />} />
            </Route>
            <Route element={<ProtectedRoutesWrapper />}>
                <Route path="applications" element={<ApplicationsAdmin />} />
                <Route path="persons" element={<Persons />} />
                <Route path="addPerson" element={<AddPerson />} />
                <Route path="editPerson/:id" element={<EditPerson />} />
                <Route path="locations" element={<Locations />} />
                <Route path="addLocation" element={<AddLocation />} />
                <Route path="editLocation/:id" element={<EditLocation/>} />
                <Route path="coordinates" element={<Coordinates/>} />
                <Route path="addCoordinate" element={<AddCoordinate />} />
                <Route path="editCoordinate/:id" element={<EditCoordinate/>} />
                <Route path="functions" element={<Functions/>} />
                <Route path="/import-persons" element={<ImportPersons />} />
                <Route path="/import-history" element={<ImportHistory/>} />
            </Route>
        </Route>
    )
);

const App = () => {
    return (
        <RouterProvider router={router} />
    );
};

export default App;