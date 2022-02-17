import axios from "./AxiosInstance";

/**
 * @remarks
 * Used for connecting to the webhost.
 * 
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
const connectToHost = async () => {
    try {
        const response = await axios.post(`connect`, {
        hostUrl: "http://localhost:8080",
        constraint:
            "C:6C5AE57C0D779D635FBF5227CE1DEC4A0736BD5F02CC8E8E6DB61F76DE56C1F0 SEC:INSECURE",
    });
    return response;
}
    catch (error) {
        throw error;
    }
};

/**
 * @remarks
 * Used for getting auction poll information (null if auction is ongoing).
 * 
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
const getPollResponse = async () => {
    try {
        const response = await axios.post(`poll`);
    return response;
}
    catch (error) {
        throw error;
    }
};

/**
 * @remarks
 * Used for getting auction poll information (null if auction is ongoing).
 * 
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
const sendBid = async (roleType: string, bid: string | number) => {
    try {
        const response = await axios.post(`bid`, {
            roleType: roleType,
            bid: bid
        });
    return response;
}
    catch (error) {
        throw error;
    }
};

/**
 * @remarks
 * Used for endingAuction, only admin can call
 * 
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
const endAuction = async () => {
    try {
        const response = await axios.post(`endAuction`);
    return response;
}
    catch (error) {
        throw error;
    }
};
  
export default { connectToHost, getPollResponse, sendBid, endAuction };