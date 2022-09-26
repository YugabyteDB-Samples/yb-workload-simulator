export interface YBServerModel {
    host : string;
    port : string;
    cloud : string;
    region : string;
    zone: string;
    inetServer : string;
    nodeUp : boolean;
    master : boolean;
    tserver: boolean;
    readReplica : boolean;
}