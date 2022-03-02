import { Navigate, useLocation } from "react-router";

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  let currentSession = sessionStorage.getItem("user");

  let location = useLocation();
  if (currentSession === null) {
    return <Navigate to="/" state={{ from: location }} />;
  } else return children;
};

export default ProtectedRoute;
