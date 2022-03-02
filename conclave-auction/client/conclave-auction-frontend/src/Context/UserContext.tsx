import { createContext, useContext } from "react";

export type UserContextType = {
  user: string | any;
  setUser: (value: string) => void;
};
let currentSession = sessionStorage.getItem("user");

export const UserContext = createContext<UserContextType>({
  user: currentSession,
  setUser: (value) => console.log("No User here", value),
});

export const useUser = () => useContext(UserContext);
