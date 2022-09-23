import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import * as d3 from 'd3';
import { SimulationNodeDatum, ForceLink, SimulationLinkDatum } from 'd3';
import { YBServerModel } from 'src/app/model/yb-server-model.model';
import { YugabyteDataSourceService } from 'src/app/services/yugabyte-data-source.service';
import { ConfirmationService, ConfirmEventType, MessageService } from 'primeng/api';

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
  styleUrls: ['./network-diagram.component.css'],
  providers: [ConfirmationService, MessageService]

})

export class NetworkDiagramComponent implements OnInit, AfterViewInit, OnChanges {
  private svg : any;
  private rootElement : any;
  private width = 0;
  private height = 0;
  private simulation : any;
  private colorMap : any;
  private currentNodes  : NetworkNode[] = []; 
  private timer : any;

  private d3link : any = d3;
  status = "0 nodes selected";

  selectedItems : NetworkNode[] = []; // A set of the selected items with the id as the key

  graphNodes: NetworkNode[] = [];
  graphLinks: NetworkLink[] = [];
  nodeCount = 0;
  missingNodes = [];
  
  @Input()
  yugabyteOffering = 'Yugabyte MDB';

  @ViewChild('network')
  network! : ElementRef;

  @Input()
  graphRefreshMs = 1000;

  constructor(
    private ybServer : YugabyteDataSourceService,
    private confirmService : ConfirmationService,
    private messageService : MessageService
  ) { 
  }

  ngOnInit(): void {
    console.log(this.d3link);
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
    if (changes.yugabyteOffering) {
      this.yugabyteOffering = changes.yugabyteOffering.currentValue;
    }
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

    this.nodeCount = 0;
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
      this.nodeCount++;
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

  private getNetworkNodeById(id : string) : NetworkNode | null {
    for (let i = 0; i < this.graphNodes.length; i++) {
      if (this.graphNodes[i].id === id) {
        return this.graphNodes[i];
      }
    }
    return null;
  }


  private selectedNodesChanged() {
    console.log("Selection Changed", this.selectedItems);
  }

  private getServerChildrenOrSelf(node : NetworkNode) : NetworkNode[] {
    if (node.type === NodeType.NODE) {
      return [node];
    }
    else {
      let results : NetworkNode[] = [];
      for (let i = 0; i < this.graphLinks.length; i++) {
        if (this.graphLinks[i].source === node.id) {
          let thisChild = this.getNetworkNodeById(this.graphLinks[i].target);
          if (thisChild) {
            results = results.concat(this.getServerChildrenOrSelf(thisChild));
          }
        }
      }
      return results;
    }
  }

  private isNodeSelected( node : NetworkNode ) {
    for (let i = 0; i < this.selectedItems.length; i++) {
      if (this.selectedItems[i].id === node.id) {
        return true;
      }
    }
    return false;
  }

  // Determine if the item is already selected. If the item is a node, this is obvious,
  // but if the item is a zone or region, it's only considered selected if all children are selected.
  private isSelected(target: any ) : boolean {
    let targetData : NetworkNode = d3.select(target).data()[0] as NetworkNode;
    let allChildren = this.getServerChildrenOrSelf(targetData);
    for (let i = 0; i < allChildren.length; i++) {
      if (!this.isNodeSelected(allChildren[i])) {
        return false;
      }
    }
    return true;
  }

  private setSelected(target: any) {
    if (!this.isSelected(target)) {
      let targetData : NetworkNode = d3.select(target).data()[0] as NetworkNode;
      let nodes = this.getServerChildrenOrSelf(targetData);
      for (let i = 0; i < nodes.length; i++) {
        if (!this.isNodeSelected(nodes[i])) {
          d3.selectAll('.server').filter(d => (d as NetworkNode).id == nodes[i].id).classed("selected", true);
          this.selectedItems.push(nodes[i]);
        }
      }
      this.selectedNodesChanged();
    }
  }

  private setDeselected(target: any) {
    let targetData : NetworkNode = d3.select(target).data()[0] as NetworkNode;
    let nodes = this.getServerChildrenOrSelf(targetData);
    for (let i = nodes.length-1; i>= 0; i--) {
      d3.selectAll('.server').filter(d => (d as NetworkNode).id == nodes[i].id).classed("selected", false);
      this.selectedItems.splice(i, 1);
    }
    this.selectedNodesChanged();
  }

  private toggleSelection(target:any) {
    if (this.isSelected(target)) {
      this.setDeselected(target);
    }
    else {
      this.setSelected(target);
    }
  }

  private deselectAll() {
    d3.selectAll(".server").classed("selected", false);
    this.selectedItems = [];   
    this.selectedNodesChanged();
  }

  private click(d : PointerEvent) {
    let target : any = d.currentTarget;
    let shiftKey = d.shiftKey;
    if (shiftKey) {
      this.toggleSelection(target);
    }
    else {
      this.deselectAll();
      this.setSelected(target);
    }
  }

  formComputerImage() : any {
    let yOffset = -40;
    let width = 80;
    let height = 26;
    let cornerRadius = 8;
    let path = d3.path();
    path.moveTo(-width/2, yOffset+cornerRadius);
    path.arc(-width/2+cornerRadius, yOffset+cornerRadius, cornerRadius, Math.PI, Math.PI*3/2, false);
    path.lineTo(width/2-cornerRadius, yOffset);
    path.arc(width/2-cornerRadius, yOffset+cornerRadius, cornerRadius, Math.PI * 3/2, Math.PI * 2, false);
    path.lineTo(width/2, yOffset + height - cornerRadius);
    path.arc(width/2-cornerRadius, yOffset + height-cornerRadius, cornerRadius, 0, Math.PI * 1/2, false);
    path.lineTo(-width/2+cornerRadius,yOffset + height);
    path.arc(-width/2+cornerRadius,yOffset + height-cornerRadius, cornerRadius, Math.PI/2, Math.PI, false);
    path.closePath();

    let centerX = -width/2 + width/5;
    let centerY = yOffset + height/2; 
    let radius = 8;
    path.moveTo( centerX+ radius, centerY);
    path.arc(centerX, centerY, radius, 0, Math.PI, false);
    path.arc(centerX, centerY, radius, Math.PI, 2*Math.PI, false);
    for (let i = 0; i < 5; i++) {
      let xLoc = width/2 - 8*(i+1);
      path.moveTo(xLoc, yOffset + height *0.2);
      path.lineTo(xLoc, yOffset + height *0.8);
    }

    return path;
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

    var computers = node.filter((d:NetworkNode) => {return d.type == NodeType.NODE})
        .append("path")
        .attr("class", "server")
        .on("click", (d:any) => {this.click(d)})
        .attr("d", this.formComputerImage())
        .attr("stroke", "white")
        .attr("stroke-width", 3)

    var circles = node.filter((d:NetworkNode) => {return d.type != NodeType.NODE})
      .append("circle")
      .on("click", (d:any) => {this.click(d)})
      .attr("class", (d: NetworkNode) => d.type)
      .attr("r", (d: NetworkNode) => { return this.nodeRadius(d);})
      .attr("fill", (d: NetworkNode) => { return this.nodeColor(d);});
    
    var labels = node.append("text")
      .text(function(d: { id: any; }) { return d.id;})
      .attr('fill', 'white')
      .attr('font-size', (d: NetworkNode) => { return this.nodeFontSize(d);})
      .attr('text-anchor', 'middle');
  
    node.append("title")
      .text(function(d: { id: any; }) { return d.id; });
    node.exit().remove();
    this.simulation
      .nodes(this.graphNodes)
      .on("tick", ticked);
  
    let x = this.simulation
      .force("charge", d3.forceManyBody().strength(-800))
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

  confirmStopNodes() {
    let message = `Are you sure that you want to stop ${this.selectedItems.length} selected node`;
    message = message + (this.selectedItems.length == 1 ? '?' : 's?'); 
    this.confirmService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        let nodeIds = this.selectedItems.map(d => d.id);
        this.messageService.add({severity:'info', summary:'Request Sent', detail:'Requesting nodes to be stopped...', key: 'tc'});
        this.ybServer.stopNodes(nodeIds).subscribe(result => {
          console.log(result);
          this.messageService.add({severity:'info', summary:'Success', detail:'Successfully requested nodes to be stopped', key: 'tc'});
        }, 
        error => {
          console.log("error stopping nodes " + nodeIds, error);
          this.messageService.add({severity:'error', summary:'Error stopping node(s)', detail:'An error occurred submitting the request. The error returned was: ' + error, key: 'tc'});
        }) ;
      },
      reject: () => {
          // switch(type) {
          //     case ConfirmEventType.REJECT:
                  this.messageService.add({severity:'error', summary:'Rejected', detail:'You have rejected'});
              // break;
              // case ConfirmEventType.CANCEL:
              //     this.messageService.add({severity:'warn', summary:'Cancelled', detail:'You have cancelled'});
              // break;
          // }
      }
    });
  }
}
