import React, { useContext, useEffect, useState } from "react";
import "./MainSidebar.scss";
import conclaveLogo from "../../Assets/Asset_1.svg";
import { useNavigate } from "react-router";
import APIService from "../../API/APIService";
import { ConnectionContext } from "../../Context/ConnectionContext";
import { MessageContext } from "../../Context/MessageContext";

interface Props {
  messageCounter: number;
  setMessageCounter: (value: number) => void;
}

const MainSidebar: React.FC<Props> = ({
  messageCounter,
  setMessageCounter,
}) => {
  const [dateYear] = useState(new Date().getFullYear());
  const { isConnected, setIsConnected } = useContext(ConnectionContext);
  const { messages, setMessages } = useContext(MessageContext);

  let currentUser = sessionStorage.getItem("user");
  let currentConnection = sessionStorage.getItem("isConnected");

  const history = useNavigate();

  const connectToHost = async () => {
    try {
      const response = await APIService.connectToHost();
      setIsConnected(true);
      sessionStorage.setItem("isConnected", `true`);
      addNewMessage(`${response.data.message}`, "host");
      return response;
    } catch (error) {
      addNewMessage(`${error}`, "host");
      console.log("ERROR: ", error);
    }
  };

  const addNewMessage = (message: string, from: string) => {
    let sessionMessages: string | null;
    if (sessionStorage.getItem("messages") !== null) {
      sessionMessages = sessionStorage.getItem("messages");
      setMessages(sessionMessages ? JSON.parse(sessionMessages) : "");
      messages.push({ message: `${message}`, from: `${from}` });
      setMessageCounter(messageCounter + 1);
    }
    sessionStorage.setItem("messages", `${JSON.stringify(messages)}`);
  };

  useEffect(() => {
    if (currentConnection !== null) {
      setIsConnected(true);
    } else setIsConnected(false);
  }, [currentConnection]);

  return (
    <>
      <section className="navbar-information">
        <h1>CONCLAVE AUCTION</h1>
        <h2 className="user-information">
          <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path
              fill="currentColor"
              d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"
            />
          </svg>{" "}
          {currentUser}
        </h2>
        <button
          disabled={isConnected ? true : false}
          className={isConnected ? "is-connected-button" : ""}
          onClick={() => {
            connectToHost();
          }}
        >
          {isConnected ? `Connected` : `Connect`}
          {isConnected && (
            <svg viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="M19.78,2.2L24,6.42L8.44,22L0,13.55L4.22,9.33L8.44,13.55L19.78,2.2M19.78,5L8.44,16.36L4.22,12.19L2.81,13.55L8.44,19.17L21.19,6.42L19.78,5Z"
              />
            </svg>
          )}
        </button>
        <button
          className="logout-button"
          onClick={() => {
            sessionStorage.removeItem("user");
            sessionStorage.removeItem("isConnected");
            sessionStorage.removeItem("messages");
            history("/");
          }}
        >
          Logout
        </button>
      </section>
      <section className="copyright-information">
        <img className="conclave-logo" src={conclaveLogo} alt="logo" />
        <p>
          <small>&#169; {dateYear} R3. All rights reserved.</small>
        </p>
      </section>
    </>
  );
};

export default MainSidebar;
