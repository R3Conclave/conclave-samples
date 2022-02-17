import React, { useState } from "react";
import { ConnectionContext } from "../../Context/ConnectionContext";
import { MessageContext } from "../../Context/MessageContext";
import LeftSidebar from "../../Views-Components/LeftSidebar/LeftSidebar";
import MainSidebar from "../../Views-Components/MainSidebar/MainSidebar";
import MiddleSection from "../../Views-Components/MiddleSection/MiddleSection";
import "./MainView.scss";

const MainView: React.FC = () => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [messages, setMessages] = useState<[]>([]);
  const [messageCounter, setMessageCounter] = useState<number>(0);

  return (
    <section className="main-view">
      <ConnectionContext.Provider value={{ isConnected, setIsConnected }}>
        <MessageContext.Provider value={{ messages, setMessages }}>
          <section className="main-sidebar-section">
            <MainSidebar
              messageCounter={messageCounter}
              setMessageCounter={setMessageCounter}
            />
          </section>
          <section className="middle-section">
            <MiddleSection
              messageCounter={messageCounter}
              setMessageCounter={setMessageCounter}
            />
          </section>
          <section className="right-sidebar-section">
            <LeftSidebar
              messageCounter={messageCounter}
              setMessageCounter={setMessageCounter}
            />
          </section>
        </MessageContext.Provider>
      </ConnectionContext.Provider>
    </section>
  );
};

export default MainView;
