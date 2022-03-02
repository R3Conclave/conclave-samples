import { createContext, useContext } from "react";

export type ConnectionContextType = {
  isConnected: boolean | null;
  setIsConnected: (value: boolean) => void;
};

export const ConnectionContext = createContext<ConnectionContextType>({
  isConnected: null,
  setIsConnected: (value) => console.log("isConnected", value),
});

export const useConnection = () => useContext(ConnectionContext);
