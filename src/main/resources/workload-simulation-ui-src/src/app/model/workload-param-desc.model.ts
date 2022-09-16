import { ParamValue } from "./param-value.model";

export interface WorkloadParamDesc {
    name : string;
    type : string;
    minValue : number;
    maxValue : number;
    defaultValue : ParamValue;
    choices : String[];
    sliderLabel : String;
}