export interface TimingPoint {
    numSucceeded : number;
    numFailed : number;
    startTimeMs : number;
    minUs : number;
    maxUs : number;
    avgUs : number;
}