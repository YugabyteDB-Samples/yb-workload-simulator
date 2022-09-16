import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnInit, ViewChild, SimpleChanges } from '@angular/core';
import * as d3 from 'd3';
import { YugabyteDataSourceService } from 'src/app/services/yugabyte-data-source.service';
import { TimingData } from '../../model/timing-data.model';
import { TimingPoint } from '../../model/timing-point.model';

enum LineType { MIN = 0, AVG = 1, MAX = 2 };
@Component({
  selector: 'app-statistics-graph',
  templateUrl: './statistics-graph.component.html',
  styleUrls: ['./statistics-graph.component.css']
})
export class StatisticsGraphComponent implements OnInit, AfterViewInit, OnChanges {
  private data : TimingPoint[] = [];
  private svg : any;
  private margin = 50;
  private width = 750 - (this.margin * 2);
  private height = 400 - (this.margin * 2);
  private margins = {top: 30, left: 50, right: 20, bottom: 50};
  private xScale : any;
  private xAxis : any;
  private yScale : any;
  private yAxis : any;
  private $xAxis : any;
  private $yAxis : any;
  private area : any;
  private line : any;
  private lineMin : any;
  private lineMax : any;
  private $area : any;
  private $data : any;
  private $dataMin : any;
  private $dataMax : any;
  private $tooltip : any;
  private minTime : number = 0;
  private visibilities : { [key in LineType] : boolean } = {0 : true, 1 : true, 2: true};
  private dateFormatter = d3.timeFormat('%H:%M:%S');
  // private minVisible = true;
  // private avgVisible = true;
  // private maxVisible = true;

  @Input()
  duration = 3 * 60 * 1000;

  @ViewChild('graph')
  throughput! : ElementRef;

  @Input()
  idName = 'chart';

  @Input()
  timingData : any = {};

  // The sub-part of the timingData which this graph should look at.
  // TODO: this is not used and should be removed.
  @Input()
  timingMetric = "WORKLOAD";

  // Whether to display the LATENCY or THROUGHPUT graph. M
  @Input()
  timingType = "LATENCY";

  // The name of the graph to be displayed in the Title bar
  @Input()
  timingMetricName = "Workload";


  constructor(
    private ybServer : YugabyteDataSourceService
  ) { 
  }

  ngOnInit(): void {
    this.ybServer.getServerNodes().subscribe(node => console.log(node));
  }

  ngAfterViewInit(){
    let interval = this.timingMetricName === 'Aggregation Counter' ? 1200 : 0;
    setTimeout( () => {
      this.width = this.throughput.nativeElement.offsetWidth;
      this.height = this.throughput.nativeElement.offsetHeight;
      console.log(this.width, this.height);
      console.log(window.devicePixelRatio);
      console.log(this.throughput.nativeElement.clientWidth);
      console.log(this.throughput.nativeElement.scrollWidth);

      if (this.timingType == "LATENCY") {
        this.visibilities[LineType.MAX] = false;
      }
      this.createSvg();
      this.defineHeading();
      this.defineLabels();
      this.defineAxes();
      this.update();
      this.defineLegend();
    }, interval);
  }

  ngOnChanges(changes : SimpleChanges) {
    if (changes.timingMetricName) {
      this.timingMetricName = changes.timingMetricName.currentValue;
      if (this.svg) {
        let heading = this.getHeadingString();
        this.svg.select('.chartTitle').text(heading);
        }
    }

    this.update();
  }

  private createSvg() : void {
    this.svg = d3.select("figure#"+this.idName)
      .append("svg")
      .attr("width", "100%")
      .attr("height", "100%")
      .attr("viewBox", "0 0 " + this.width + " " + this.height)
      .attr("preserveAspectRatio", 'xMinYMin');
  }

  private getHeadingString() : string {
    if (this.timingMetricName) {
      return (this.timingType == "LATENCY" ? "Latency " : "Throughput ") + "(" + this.timingMetricName + ")";
    }
    else {
      return (this.timingType == "LATENCY" ? "Latency " : "Throughput ");
    }
  }

  private defineHeading() {
    var xLabel = this.svg.append('g').attr('class', 'xLabel heading');
    let heading = this.getHeadingString();
    xLabel.append('text')
      .attr('class', 'chartTitle')
      .attr('text-anchor', 'middle')
      .attr('x', this.width/2)
      .attr('y', 20)
      .text(heading)
      .attr('font-size', '1.5em');
  }

  private defineLegend() {
    let boxHeight = 25;
    let boxWidth = 250;
    let xLabel = this.svg.append('g')
        .attr('class', 'legend')
        .attr('width', boxWidth)
        .attr('height', boxHeight)
        // .attr('transform', 'translate(10,10)')
        .attr('transform', 'translate(' +(this.width-260) + ',0)');
    if (this.width < 600) {
      xLabel.attr('visibility', 'hidden');
    }
    let box = xLabel.append('rect')
        .attr('x', 0)
        .attr('y', 1)
        .attr('width', boxWidth)
        .attr('height', boxHeight)
        .attr('stroke', 'white')
        .attr('fill', 'none');

    let labels = [];
    if (this.timingType =="LATENCY") {
      labels = ["Min", "Avg", "Max"];
    }
    else {
      labels = ["Failed", "Success", "Total"];
    }
    for (let i = 0; i < labels.length; i++) {
      let startX = (boxWidth / labels.length) * i;
      let classType = (i == 0) ? 'lineMin' : (i == 1) ? 'line' : 'lineMax';
      xLabel.append("line") 
        .attr("x1", startX + 12)
        .attr("y1", boxHeight/2)      
        .attr("x2", startX + 30) 
        .attr("y2", boxHeight/2) 
        .attr('class', 'data ' + classType);
      let textItem = xLabel.append('text')
        .attr('class', 'legendLabel ' + classType)
        .attr('text-anchor', 'start')
        .attr('x', startX + 34)
        .attr('y', 18)
        .attr('font-size', '1em')
        .attr('fill', 'white')
        .text(labels[i])
        .on("click", (evt : any, x: any) => { 
          let thisObj = d3.select(evt.srcElement);
          this.toggleVisibility(LineType.AVG, thisObj);
          this.toggleVisibility(LineType.MIN, thisObj);
          this.toggleVisibility(LineType.MAX, thisObj);
        });

      // If we need to default a visibility to false, set it true then toggle it to false.
      if (!this.visibilities[i as LineType]) {
        this.visibilities[i as LineType] = true;
        this.toggleVisibility(i as LineType, textItem);
      }
    }
  }

  private toggleVisibility(type : LineType, element : d3.Selection<any, any, any, any>) {
    let classString;
    switch (type) {
      case LineType.AVG: classString = "line"; break;
      case LineType.MIN: classString = "lineMin"; break;
      case LineType.MAX: classString = "lineMax"; break;
    }
    let isThisElement = element.classed(classString);
    if (isThisElement) {
      let newOpacity;
      if (this.visibilities[type]) {
        element.style("opacity", 0.5);
        newOpacity = 0;
      }
      else {
        element.style("opacity", 1);
        newOpacity = 1;
      }
      this.svg.select("path.data."+classString).style("opacity", newOpacity);
      this.visibilities[type] = !this.visibilities[type];
    }
  }

  private defineLabels() {
    var xLabel = this.svg.append('g').attr('class', 'xLabel');
    xLabel.append('text')
      .attr('class', 'chartTitle')
      .attr('x', this.width/2)
      .attr('y', this.height - 10)
      .text('Time')
      .attr('font-size', '1em');

    var yLabel = this.svg.append('g').attr('class', 'xLabel');
    yLabel.append('text')
      .attr('class', 'chartTitle')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('font-size', '1em')
      .attr('x', -this.margins.top-160)
      .attr('y', -this.margins.left+65)
      .text(this.timingType == 'LATENCY' ? 'Latency (ms)' : 'Count');
  }

  private defineAxes() {
    this.xScale = d3.scaleTime().range([this.margins.left, this.width - this.margins.right]);
    this.xAxis = d3.axisBottom<Date>(this.xScale)
      .tickFormat(d3.timeFormat('%H:%M:%S'))
      .ticks(15)
      .tickSizeInner(-this.height + this.margins.bottom + this.margins.top)
      .tickSizeOuter(6)
      .tickPadding(5);

    this.yScale = d3.scaleLinear().range([this.height - this.margins.bottom, this.margins.top]);
    this.yAxis = d3.axisLeft<number>(this.yScale)
      .tickFormat(d3.format('.2s'))
      .tickSizeInner(-this.width + this.margins.left + this.margins.right)
      .tickSizeOuter(5)
      .tickPadding(8);

    if (this.timingType == "LATENCY") {
      this.line = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getY(d as unknown as TimingPoint)));

      this.lineMax = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getMaxY(d as unknown as TimingPoint)));

      this.lineMin = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getMinY(d as unknown as TimingPoint)));


      this.area = d3.area()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y0(this.height-this.margins.bottom)
        .y1((d, i) => this.yScale(this.getY(d as unknown as TimingPoint)));
    }
    else {
      this.line = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getSuccessfulTxns(d as unknown as TimingPoint)));

      this.lineMax = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getTotalTxns(d as unknown as TimingPoint)));

      this.lineMin = d3.line()
        // .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y((d, i) => this.yScale(this.getFailedTxns(d as unknown as TimingPoint)));

      this.area = d3.area()
        .curve(d3.curveBasis)
        .x((d, i) => this.xScale(this.getX(d as unknown as TimingPoint)))
        .y0(this.height-this.margins.bottom)
        .y1((d, i) => this.yScale(this.getY(d as unknown as TimingPoint)));
    }

    this.$xAxis = this.svg.append('g').attr('class', 'x axis')
      .attr('transform', `translate(0, ${this.height-this.margins.bottom})`)
      .call(this.xAxis);
    this.$yAxis = this.svg.append('g').attr('class', 'y axis')
      .attr('transform', `translate(${this.margins.left})`)
      .call(this.yAxis);

    var clip = this.svg.append("defs").append("svg:clipPath")
      .attr("id", "clip")
      .append("svg:rect")
      .attr("id", "clip-rect")
      .attr("x", this.margins.left+1)
      .attr("y", this.margins.top)
      .attr("width", this.width-this.margins.left - this.margins.right+1)
      .attr("height", this.height - this.margins.top - this.margins.bottom);
						
    var visCont = this.svg.append('g')
            .attr("clip-path", "url(#clip)")
            .attr('class', 'vis');

    this.$data = visCont.append('path').attr('class', 'line data');
    this.$dataMin = visCont.append('path').attr('class', 'lineMin data');
    this.$dataMax = visCont.append('path').attr('class', 'lineMax data');
    this.$area = visCont.append('path').attr('class', 'area data');
    this.createToolTip();
  }

  private update() {
    if (!this.xScale || !this.timingData ) {
      return;
    }
    this.data = (this.timingData);
    let now = Date.now();
    this.minTime = now - this.duration - 1000;
    this.xScale.domain([now - this.duration, now]);
    let y = 100;
    if (this.data) {
      y = this.getHighestVisibleY();
    }
    this.yScale.domain([0, y]);
    this.$xAxis.call(this.xAxis);
    this.$yAxis.call(this.yAxis);
    if (this.data) {
      if (this.timingType == "LATENCY") {
        this.$area.datum(this.data).attr('d', this.area);
      }
      this.$data.datum(this.data).attr('d', this.line);
      this.$dataMax.datum(this.data).attr('d', this.lineMax);
      this.$dataMin.datum(this.data).attr('d', this.lineMin);
    }
  }

  private getHighestVisibleY() : number {
    if (this.timingType == "LATENCY") {
      let fn = this.getMinY;
      if (this.visibilities[LineType.MAX]) {
        fn = this.getMaxY;
      }
      else if (this.visibilities[LineType.AVG]) {
        fn = this.getY;
      }
      return (d3.max(this.data, d => fn.call(this, d)) || 0) + 1;
    }
    else {
      return (d3.max(this.data, d => this.getTotalTxns(d)) || 0) + 1;
    }
  }
  
  private validatePoint(point : TimingPoint) : boolean {
    if (!point || !point.startTimeMs) return false;
    if (this.minTime > 0) return point.startTimeMs >= this.minTime;
    return true;
  }
  private getX(point : TimingPoint) : number {
    return point ? point.startTimeMs : 0;
  }

  private getHighestYFromPoint(point : TimingPoint) : number {
    if (this.timingType == 'LATENCY') {
      let fn = this.getMinY;
      if (this.visibilities[LineType.MAX]) {
        fn = this.getMaxY;
      }
      else if (this.visibilities[LineType.AVG]) {
        fn = this.getY;
      }
      return fn.call(this, point);
    }
    else {
      let fn = this.getFailedTxns;
      if (this.visibilities[LineType.MAX]) {
        fn = this.getSuccessfulTxns;
      }
      else if (this.visibilities[LineType.AVG]) {
        fn = this.getTotalTxns;
      }
      return fn.call(this, point);
    }
  }
  private getMaxY(point : TimingPoint, minTime? : number) : number {
    if (this.validatePoint(point)) {
      return point.maxUs / 1000.0;
    }
    return 0;
  }

  private getY(point : TimingPoint) : number {
    return this.validatePoint(point) ? point.avgUs/1000.0 : 0;
  }

  private getMinY(point : TimingPoint) : number {
    return this.validatePoint(point) ? point.minUs/1000.0 : 0;
  }

  private getTotalTxns(point : TimingPoint, minTime? : number) : number {
    return this.validatePoint(point) ? point.numFailed+point.numSucceeded : 0;
  }

  private getSuccessfulTxns(point : TimingPoint) : number {
    return this.validatePoint(point) ? point.numSucceeded : 0;
  }

  private getFailedTxns(point : TimingPoint) : number {
    return this.validatePoint(point) ? point.numFailed : 0;
  }

  private formatToOneDP(num : number) : number {
    return Math.round(num * 10) / 10;
  }
  private formatLatency(point : TimingPoint) : string {
    return "Min:"+this.formatToOneDP(point.minUs/1000.0) + ", Avg:" + this.formatToOneDP(point.avgUs/1000.0) + ", Max:" + this.formatToOneDP(point.maxUs/1000.0);
  }

  private formatThroughput(point : TimingPoint) : string {
    return "Throughput:" + (point.numSucceeded + point.numFailed) + " (" + point.numSucceeded + ", "+point.numFailed +")";
  }

  private createToolTip() {
    this.$tooltip = this.svg.append('g')
        .attr('class', 'focus')
        .style('display', 'none');

    this.$tooltip.append('circle')
        .attr('r', 5);

    this.$tooltip.append('rect')
        .attr('class', 'tooltip')
        .attr('width', 200)
        .attr('height', 70)
        .attr('x', -210)
        .attr('y', -22)
        .attr('rx', 4)
        .attr('ry', 4);

    this.$tooltip.append('text')
        .attr('class', 'tooltip-date')
        .attr('x', -202)
        .attr('y', -2);

    // this.$tooltip.append('text')
    //     .attr('x', 18)
    //     .attr('y', 18)
    //     .text('Likes:');

    this.$tooltip.append('text')
        .attr('class', 'tooltip-latency')
        .attr('x', -202)
        .attr('y', 18);

    this.$tooltip.append('text')
        .attr('class', 'tooltip-throughput')
        .attr('x', -202)
        .attr('y', 38);

    this.svg.append('rect')
        .attr('class', 'overlay')
        .style('fill','none')
        .style('pointer-events', 'all')
        .attr('width', this.width)
        .attr('height', this.height)
        .on('mouseover', () => this.$tooltip.style('display', null))
        .on('mouseout', () => this.$tooltip.style('display', 'none'))
        .on('mousemove', (evt:MouseEvent) => {
          let cursor = d3.pointer(evt)[0];
          let date = this.xScale.invert(cursor);
          let time = date.getTime();
          let point = d3.bisector(function(d) { 
            return (d as TimingPoint).startTimeMs; 
          });
          let closest = point.left(this.data, time, 1);
          if (closest > 0 && closest < this.data.length) {
            let thisPoint = this.data[closest-1];
            let nextPoint = this.data[closest];
            let dataPoint = time - thisPoint.startTimeMs > time - nextPoint.startTimeMs ? nextPoint : thisPoint;
            if (dataPoint) {
              this.$tooltip.attr('transform', 'translate(' + this.xScale(dataPoint.startTimeMs) + ',' + this.yScale(this.getHighestYFromPoint(dataPoint)) + ')');
              this.$tooltip.select('.tooltip-date').text(this.dateFormatter(new Date(dataPoint.startTimeMs)));
              this.$tooltip.select('.tooltip-latency').text(this.formatLatency(dataPoint));
              this.$tooltip.select('.tooltip-throughput').text(this.formatThroughput(dataPoint));
            }
          }
        });
  }

}
