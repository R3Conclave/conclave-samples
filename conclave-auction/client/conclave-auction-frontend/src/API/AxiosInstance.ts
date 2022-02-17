import axios, { AxiosRequestConfig } from "axios";

axios.interceptors.request.use(async (config: AxiosRequestConfig) => {

let user = sessionStorage.getItem('user');
    let APIURL = '';
    
    switch (user) {
        case 'BIDDER1':
            APIURL = `bidder1/auction-service/`;
            break;
        case 'BIDDER2':
            APIURL = `bidder2/auction-service/`;
            break;
        case 'BIDDER3':
            APIURL = `bidder3/auction-service/`;
            break;
        case 'ADMIN':
            APIURL = `admin/auction-service/`;
            break;
        default:
            console.log("No User Selected");
    }


    config.baseURL = APIURL;

    config.headers = {
        'accept': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,PUT,POST,DELETE,PATCH,OPTIONS',
    };
    config.auth = {
        username: 'username',
        password: 'password'
      }
    return config;
});

export default axios;