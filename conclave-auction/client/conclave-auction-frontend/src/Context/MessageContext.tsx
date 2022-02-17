import { createContext, useContext } from "react";

export type MessageContextType = {
  messages: any;
  setMessages: (value: any) => void;
};

export const MessageContext = createContext<MessageContextType>({
  messages: [],
  setMessages: (value) => console.log("no messages here: ", value),
});

export const useMessages = () => useContext(MessageContext);
