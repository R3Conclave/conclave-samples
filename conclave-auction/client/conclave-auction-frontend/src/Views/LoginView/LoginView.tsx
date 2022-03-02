import React from "react";
import { useNavigate } from "react-router";
import logo2 from "../../Assets/Conclave_logo_master_b&r.svg";
import "./LoginView.scss";

const ADMIN = "ADMIN";
const BIDDER1 = "BIDDER1";
const BIDDER2 = "BIDDER2";
const BIDDER3 = "BIDDER3";

let welcomeMessage = [
  {
    message: `Welcome! Press the "Connect" button to start the Auction.`,
    from: "host",
  },
];

const LoginView: React.FC = () => {
  const history = useNavigate();

  const handleLogin = (user: string) => {
    sessionStorage.setItem("user", `${user}`);
    sessionStorage.setItem("messages", `${JSON.stringify(welcomeMessage)}`);

    history("/conclave-auction");
  };

  return (
    <section className="login-view">
      <section className="login-section">
        <section className="header-section">
          <h1>CONCLAVE AUCTION</h1>
          <div className="line"></div>
        </section>
        <section className="buttons-section">
          <button
            onClick={() => {
              handleLogin(ADMIN);
            }}
          >
            ADMIN
          </button>
          <button
            onClick={() => {
              handleLogin(BIDDER1);
            }}
          >
            BIDDER1
          </button>
          <button
            onClick={() => {
              handleLogin(BIDDER2);
            }}
          >
            BIDDER2
          </button>
          <button
            onClick={() => {
              handleLogin(BIDDER3);
            }}
          >
            BIDDER3
          </button>
        </section>
      </section>
      <img className="conclave-logo" src={logo2} alt="logo" />
    </section>
  );
};

export default LoginView;
