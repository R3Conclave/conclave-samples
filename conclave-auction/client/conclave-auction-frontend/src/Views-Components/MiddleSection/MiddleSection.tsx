import React, { useContext, useEffect } from "react";
import "./MiddleSection.scss";
import { ConnectionContext } from "../../Context/ConnectionContext";
import { MessageContext } from "../../Context/MessageContext";

interface Props {
  messageCounter: number;
  setMessageCounter: (value: number) => void;
}

const MiddleSection: React.FC<Props> = ({ messageCounter }) => {
  const { isConnected } = useContext(ConnectionContext);
  const { messages, setMessages } = useContext(MessageContext);

  const getSessionMessages = () => {
    let sessionMessages: string | null = "";
    if (sessionStorage.getItem("messages") !== null) {
      sessionMessages = sessionStorage.getItem("messages");
      setMessages(sessionMessages ? JSON.parse(sessionMessages) : "");
    }
  };

  useEffect(() => {
    getSessionMessages();
  }, [messageCounter]);

  return (
    <>
      <section className="auction-item-section">
        {!isConnected && (
          <>
            <h3>Welcome to Conclave Auction Demo</h3>
            <div>
              <svg viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M2.3,20.28L11.9,10.68L10.5,9.26L9.78,9.97C9.39,10.36 8.76,10.36 8.37,9.97L7.66,9.26C7.27,8.87 7.27,8.24 7.66,7.85L13.32,2.19C13.71,1.8 14.34,1.8 14.73,2.19L15.44,2.9C15.83,3.29 15.83,3.92 15.44,4.31L14.73,5L16.15,6.43C16.54,6.04 17.17,6.04 17.56,6.43C17.95,6.82 17.95,7.46 17.56,7.85L18.97,9.26L19.68,8.55C20.07,8.16 20.71,8.16 21.1,8.55L21.8,9.26C22.19,9.65 22.19,10.29 21.8,10.68L16.15,16.33C15.76,16.72 15.12,16.72 14.73,16.33L14.03,15.63C13.63,15.24 13.63,14.6 14.03,14.21L14.73,13.5L13.32,12.09L3.71,21.7C3.32,22.09 2.69,22.09 2.3,21.7C1.91,21.31 1.91,20.67 2.3,20.28M20,19A2,2 0 0,1 22,21V22H12V21A2,2 0 0,1 14,19H20Z"
                />
              </svg>
            </div>
          </>
        )}
        {isConnected && (
          <>
            <h3>LOT 1234</h3>
            <div>
              <svg viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M16,6L19,10H21C22.11,10 23,10.89 23,12V15H21A3,3 0 0,1 18,18A3,3 0 0,1 15,15H9A3,3 0 0,1 6,18A3,3 0 0,1 3,15H1V12C1,10.89 1.89,10 3,10L6,6H16M10.5,7.5H6.75L4.86,10H10.5V7.5M12,7.5V10H17.14L15.25,7.5H12M6,13.5A1.5,1.5 0 0,0 4.5,15A1.5,1.5 0 0,0 6,16.5A1.5,1.5 0 0,0 7.5,15A1.5,1.5 0 0,0 6,13.5M18,13.5A1.5,1.5 0 0,0 16.5,15A1.5,1.5 0 0,0 18,16.5A1.5,1.5 0 0,0 19.5,15A1.5,1.5 0 0,0 18,13.5Z"
                />
              </svg>
            </div>
          </>
        )}
      </section>
      <section className="auction-bids-section">
        {messages.length >= 1 && (
          <ul>
            {messages.map((message: any, index: any) => (
              <li
                key={`itemName-${index}`}
                className={`${message.from}-container`}
              >
                {message.message}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
};

export default MiddleSection;
