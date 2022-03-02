import React, { useContext, useState } from "react";
import "./LeftSidebar.scss";
import { MessageContext } from "../../Context/MessageContext";
import APIService from "../../API/APIService";
import { ConnectionContext } from "../../Context/ConnectionContext";

interface Props {
  messageCounter: number;
  setMessageCounter: (value: number) => void;
}

const LeftSidebar: React.FC<Props> = ({
  messageCounter,
  setMessageCounter,
}) => {
  const { messages, setMessages } = useContext<any>(MessageContext);
  const [bidInput, setBidInput] = useState<string | number>("");
  const { isConnected } = useContext(ConnectionContext);
  let currentUser = sessionStorage.getItem("user");

  const checkBidIsValid = () => {
    if (bidInput < 1) {
      addNewMessage("Please enter a bid amount", "active");
    }
  };

  const addNewMessage = (message: string, from: string) => {
    let sessionMessages: string | null;
    if (sessionStorage.getItem("messages") !== null) {
      sessionMessages = sessionStorage.getItem("messages");
      setMessages(sessionMessages ? JSON.parse(sessionMessages) : "");
    }
    messages.push({ message: `${message}`, from: `${from}` });
    setMessageCounter(messageCounter + 1);
    sessionStorage.setItem("messages", `${JSON.stringify(messages)}`);
  };

  const getPollResponse = async () => {
    try {
      const response = await APIService.getPollResponse();
      if (response.data.message === null) {
        addNewMessage("Auction is still active!", "active");
      } else {
        addNewMessage(`${response.data.message}`, "host");
      }
    } catch (error) {
      addNewMessage(`${error}`, "host");
      console.log("ERROR: ", error);
    }
  };

  const endAuction = async () => {
    try {
      const response = await APIService.endAuction();
      addNewMessage(`${response.data.message}`, "host");
    } catch (error) {
      addNewMessage(`${error}`, "host");
      console.log("ERROR: ", error);
    }
  };

  const sendBid = async (e: React.ChangeEvent<HTMLFormElement>) => {
    e.preventDefault();
    checkBidIsValid();
    let roleType = "";
    if (currentUser?.includes("BIDDER")) {
      roleType = "BIDDER";
    } else if (currentUser?.includes("ADMIN")) {
      roleType = "ADMIN";
    } else {
      addNewMessage("Cannot Bid without Connecting", "host");
    }
    try {
      const response = await APIService.sendBid(roleType, bidInput);
      addNewMessage(`${response.data.message} - €${bidInput}`, "client");
      setBidInput("");
    } catch (error) {
      addNewMessage(`${error}`, "host");
      console.log("ERROR: ", error);
    }
  };

  return (
    <>
      <section className="bid-form">
        {currentUser === "ADMIN" && (
          <>
            <h2>END AUCTION</h2>
            <button
              data-tooltip={
                !isConnected ? `Press "Connect" to Start the Auction` : ``
              }
              disabled={!isConnected ? true : false}
              type="button"
              className={
                !isConnected
                  ? `submit-bid-button tooltip-top`
                  : `submit-bid-button`
              }
              onClick={() => {
                endAuction();
              }}
            >
              End Auction
            </button>
          </>
        )}
        {currentUser != "ADMIN" && (
          <form onSubmit={sendBid}>
            <label>Bid Amount</label>
            <input
              disabled={!isConnected ? true : false}
              className="input-box"
              type="number"
              placeholder=""
              value={bidInput}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                setBidInput(e.target.value)
              }
            />
            <button
              data-tooltip={
                !isConnected ? `Press "Connect" to Start the Auction` : ``
              }
              disabled={!isConnected ? true : false}
              type="submit"
              className={
                !isConnected
                  ? `submit-bid-button tooltip-top`
                  : `submit-bid-button`
              }
            >
              Place Bid
            </button>
          </form>
        )}
      </section>
      <button
        data-tooltip={
          !isConnected ? `Press "Connect" to Start the Auction` : ``
        }
        disabled={!isConnected ? true : false}
        className={!isConnected ? `poll-button tooltip-top` : `poll-button`}
        onClick={() => {
          getPollResponse();
        }}
      >
        POLL
      </button>
    </>
  );
};

export default LeftSidebar;
