import "./App.scss";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import LoginView from "./Views/LoginView/LoginView";
import MainView from "./Views/MainView/MainView";
import ProtectedRoute from "./Routes/ProtectedRoute";

function App() {
  return (
    <main>
      <Router>
        <Routes>
          <Route path="/" element={<LoginView />} />
          <Route
            path="/conclave-auction"
            element={
              <ProtectedRoute>
                <MainView />
              </ProtectedRoute>
            }
          />
        </Routes>
      </Router>
    </main>
  );
}

export default App;
