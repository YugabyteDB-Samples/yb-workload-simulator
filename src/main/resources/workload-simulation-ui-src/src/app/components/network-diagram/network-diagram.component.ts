import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import * as d3 from 'd3';
import { SimulationNodeDatum, ForceLink, SimulationLinkDatum } from 'd3';
import { YBServerModel } from 'src/app/model/yb-server-model.model';
import { YugabyteDataSourceService } from 'src/app/services/yugabyte-data-source.service';

enum NodeType {
  REGION, ZONE, NODE
}
interface NetworkNode extends SimulationNodeDatum {
  id: string;
  type: NodeType;
}

interface NetworkLink extends SimulationLinkDatum<NetworkNode> {
  source: string;
  target: string;
  value: number;  // This will be used for the fatness of the strokes
}

interface FullDragEvent extends DragEvent {
  active : boolean;
  sourceEvent : MouseEvent;
}

@Component({
  selector: 'app-network-diagram',
  templateUrl: './network-diagram.component.html',
  styleUrls: ['./network-diagram.component.css']
})

export class NetworkDiagramComponent implements OnInit, AfterViewInit, OnChanges {
  private svg : any;
  private rootElement : any;
  private width = 0;
  private height = 0;
  private simulation : any;
  private colorMap : any;
  private currentNodes : NetworkNode[] = [];
  private timer : any;

  graphNodes: NetworkNode[] = [];
  graphLinks: NetworkLink[] = [];

  @ViewChild('network')
  network! : ElementRef;

  @Input()
  graphRefreshMs = 1000;

  constructor(
    private ybServer : YugabyteDataSourceService
  ) { 
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(){
    setTimeout( () =>  {
      this.width = this.network.nativeElement.offsetWidth;
      this.height = this.network.nativeElement.offsetHeight;
      console.log(this.width, this.height);

      this.createSvg();
      this.timer = setInterval(() => {
        this.ybServer.getServerNodes().subscribe(nodes => this.update(nodes));
      },this.graphRefreshMs);
    }, 1150);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.graphRefreshMs) {
      clearInterval(this.timer);
      this.timer = setInterval(() => {
        this.ybServer.getServerNodes().subscribe(nodes => this.update(nodes));
      },changes.graphRefreshMs.currentValue);
    }
  }
  
  private createSvg() : void {
    this.svg = d3.select("figure#network")
      .append("svg")
      .attr("width", "100%")
      .attr("height", "100%")
      //.attr("viewBox", '0 0 300 400')
      .attr("viewBox", "0 0 " + this.width + " " + this.height)
      .attr("preserveAspectRatio", 'xMinYMin');

    this.rootElement = this.svg.append('g').attr('class', 'root');
    let zoom = d3.zoom().on('zoom', (e) => d3.select('g.root').attr('transform', e.transform));
    this.svg.call(zoom);
  }

  private update(nodes : YBServerModel[]) {
    let regions : NetworkNode[] = [];
    let zones : NetworkNode[] = [];
    let allNodes : NetworkNode[] = [];
    let allLinks : NetworkLink[] = [];

    for (let i = 0; i < nodes.length; i++) {
      let thisNode = nodes[i];
      if (!regions.find(node => node.id == thisNode.region)) {
        let region = {id: thisNode.region, type: NodeType.REGION};
        // Add links from this region to the other regions
        for (let j = 0; j < regions.length; j++) {
          allLinks.push({source: region.id, target: regions[j].id, value: 5})
        }
        regions.push(region);
        allNodes.push(region);
        
      }

      if (!zones.find(node => node.id == thisNode.zone)) {
        let zone = {id: thisNode.zone, type: NodeType.ZONE};
        zones.push(zone);
        allNodes.push(zone);
        allLinks.push({source: thisNode.region, target: thisNode.zone, value: 3});
      }

      allNodes.push({id: thisNode.host, type: NodeType.NODE});
      allLinks.push({source: thisNode.zone, target: thisNode.host, value: 1});
    }
    this.graphLinks = allLinks;
    this.graphNodes = allNodes
    if (this.currentNodes.length != this.graphNodes.length && this.rootElement) {
      this.updateGraph();
      this.currentNodes = this.graphNodes;
    }
  }

  private nodeRadius(node : NetworkNode) : number {
    switch(node.type) {
      case NodeType.REGION: return 40;
      case NodeType.ZONE: return 30;
      default: return 15;
    }
  }

  private nodeColor(node : NetworkNode) : string {
    switch(node.type) {
      case NodeType.REGION: return this.colorMap(1);
      case NodeType.ZONE: return this.colorMap(2);
      default: return this.colorMap(3);
    }
  }

  private nodeFontSize(node : NetworkNode) : string {
    switch(node.type) {
      case NodeType.REGION: return "medium";
      case NodeType.ZONE: return "small";
      default: return "x-small";
    }
  }

  updateGraph() {
    if (!this.rootElement) {
      return;
    }
    this.rootElement.selectAll('g').remove();
    this.colorMap = d3.scaleOrdinal(d3.schemeCategory10);

    this.simulation = d3.forceSimulation()
      .force("link", d3.forceLink().id(function(d) { return (d as any).id; }))
      .force("charge", d3.forceManyBody())
      .force("center", d3.forceCenter(this.width / 2, this.height / 2));
      
    var link = this.rootElement.append("g")
      .attr("class", "links")
      .selectAll("line")
      .data(this.graphLinks)
      .enter().append("line")
      .attr("stroke", "white")
      .attr("stroke-width", function(d: { value: number; }) { return (d.value); });

    var node = this.rootElement.append("g")
      .attr("class", "nodes")
      .selectAll("g")
      .data(this.graphNodes)
      .enter().append("g")
      .call(d3.drag()   // Drag and drop handler
        .on("start", (d,node) => {
          if (!d.active) this.simulation.alphaTarget(0.3).restart();
          (node as any).fx = d.x;
          (node as any).fy = d.y;
        })
        .on("drag", (d,node) => {
          (node as any).fx = d.sourceEvent.x;
          (node as any).fy = d.sourceEvent.y;
        })
        .on("end", (d,node) => {
          if (!d.active) this.simulation.alphaTarget(0);
          (node as any).fx = null;
          (node as any).fy = null;
        }));
      
    var circles = node.append("circle")
      .attr("r", (d: NetworkNode) => { return this.nodeRadius(d);})
      .attr("fill", (d: NetworkNode) => { return this.nodeColor(d);});
    
    var labels = node.append("text")
      .text(function(d: { id: any; }) { return d.id;})
      .attr('fill', 'white')
      .attr('font-size', (d: NetworkNode) => { return this.nodeFontSize(d);})
      .attr('text-anchor', 'middle');
  
    node.append("title")
      .text(function(d: { id: any; }) { return d.id; });
  
    this.simulation
      .nodes(this.graphNodes)
      .on("tick", ticked);
  
    let x = this.simulation
      .force("charge", d3.forceManyBody().strength(-350))
      .force("link");
    if (x) {
      x.links(this.graphLinks);
    }

    function ticked() {
      link
          .attr("x1", function(d: { source: { x: number; }; }) { return d.source.x; })
          .attr("y1", function(d: { source: { y: number; }; }) { return d.source.y; })
          .attr("x2", function(d: { target: { x: number; }; }) { return d.target.x; })
          .attr("y2", function(d: { target: { y: number; }; }) { return d.target.y; });
  
      node
          .attr("transform", function(d: { x: number; y: number; }) {
            return "translate(" + d.x + "," + d.y + ")";
          });
    }
  }
}
