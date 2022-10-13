import { LoginInformation } from "./login-information.model";

export interface Configuration {
    managementType : string;
    accessKey : string;
    accountId : string;
    projectId : string;
    clusterId : string;
    login? : LoginInformation;
}