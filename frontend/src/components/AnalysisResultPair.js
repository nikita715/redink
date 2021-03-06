import React from "react";
import axios from "axios";
import * as PROP from "../properties";
import {formatDateSimple, times} from "../util";
import {Link} from "react-router-dom";

function checkLine(line) {
  return line === "" ? " " : line;
}

export class AnalysisResultPair extends React.Component {
  state = {
    analysisId: 0,
    pairId: 0,
    files1: [],
    files2: [],
    leftName: "",
    rightName: "",
    leftTime: "",
    rightTime: "",
    leftMatches: [],
    rightMatches: [],
    leftRightMatches: [],
    percentage: 0
//    scrollEnabled: false,
//    offset: 0
  };
  matchIndex = 0;
  redClass = false;

  constructor(props, context) {
    super(props, context);
    let analysisId = props.match.params.analysisId;
    let pairId = props.match.params.pairId;
    this.state.analysisId = analysisId;
    this.state.pairId = pairId;
    axios.get(PROP.serverUrl + "/api/analyzes/" + analysisId + "/pairs/" + pairId).then((response) => {
      let files1 = response.data.files1.map((file) => ({fileName: file.name, lines: file.content}));
      let files2 = response.data.files2.map((file) => ({fileName: file.name, lines: file.content}));

      let leftMatches = [];
      let rightMatches = [];
      let leftRightMatches = [];
      response.data.pair.lines.map((match) => {
        let from1 = match.from1;
        let to1 = match.to1;
        let from2 = match.from2;
        let to2 = match.to2;
        leftMatches.push(from1);
        leftMatches.push(to1);
        rightMatches.push(from2);
        rightMatches.push(to2);
        leftRightMatches.push({from1, to1, from2, to2});
        return null;
      });

      this.setState({
        files1,
        files2,
        leftName: response.data.pair.student1,
        rightName: response.data.pair.student2,
        leftTime: formatDateSimple(response.data.pair.createdAt1),
        rightTime: formatDateSimple(response.data.pair.createdAt2),
        leftMatches,
        rightMatches: rightMatches.sort(function (a, b) {
          return parseInt(a) - parseInt(b);
        }),
        percentage: response.data.pair.percentage,
        leftRightMatches
      });
    });
    this.getLineClass = this.getLineClass.bind(this);
    this.getHrefToLine = this.getHrefToLine.bind(this);
    this.scrollTo = this.scrollTo.bind(this);
    this.findToHref = this.findToHref.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.scrollToClicked = this.scrollToClicked.bind(this);
//    this.handleScroll = this.handleScroll.bind(this);
  }

//  lastScroll = null;
//
//  handleScroll(event) {
//    let previousOffset = this.state.offset;
//    let currentOffset = event.nativeEvent.target.scrollTop;
//    let previousTarget = this.lastScroll;
//    let currentTarget = event.nativeEvent.target.id;
//    this.lastScroll = currentTarget;
//    if (currentOffset !== undefined && previousTarget === currentTarget) {
//      this.setState({offset: currentOffset});
//      if (this.state.scrollEnabled) {
//        let side = event.nativeEvent.target.id === "left-compare" ? document.getElementById("right-compare") : document.getElementById("left-compare");
//        side.scrollTop = side.scrollTop + (currentOffset - previousOffset);
//      }
//    }
//  }

  getLineClass(matches, lineIndex) {
    let lineClass = "";
    if (lineIndex === 1) {
      this.matchIndex = 0;
      this.redClass = false;
    }
    if (lineIndex === 1 && this.matchIndex === 1) {
      lineClass += " top-red-line";
    }
    if (matches[this.matchIndex] === lineIndex && !this.redClass) {
      this.redClass = !this.redClass;
      this.matchIndex += 1;
      lineClass += " top-red-line";
    } else if (lineIndex - matches[this.matchIndex] === 1 && this.redClass) {
      this.redClass = !this.redClass;
      this.matchIndex += 1;
      if (lineIndex - matches[this.matchIndex] === 1 || lineIndex === matches[this.matchIndex]) {
        this.redClass = !this.redClass;
        this.matchIndex += 1;
        lineClass += " top-red-line";
      }
    }
    if (lineIndex - matches[this.matchIndex] === 0 && this.redClass) {
      lineClass += " bottom-red-line";
    }
    return (this.redClass ? "red-line" : "") + lineClass;
  }

  getHrefToLine(side, line) {
    return this.redClass ? side + this.findToHref(side, line) : null;
  }

  findToHref(side, line) {
    if (side === "right") {
      return this.state.leftRightMatches.find(function (el) {
        return el.from1 <= line && el.to1 >= line;
      }).from2;
    } else {
      return this.state.leftRightMatches.find(function (el) {
        return el.from2 <= line && el.to2 >= line;
      }).from1;
    }
  }

  scrollTo(event) {
    let elementById = document.getElementById(event.target.getAttribute("to"));
    if (typeof elementById !== "undefined" && elementById !== null) {
      elementById.scrollIntoView();
      let elementById2 = document.getElementById(elementById.getAttribute("to"));
      if (typeof elementById2 !== "undefined" && elementById2 !== null) {
        elementById2.scrollIntoView();
      }
    }
  }

  scrollToClicked(event) {
    event.target.scrollIntoView();
  }

  handleChange() {
    this.setState({scrollEnabled: !this.state.scrollEnabled});
  }

  render() {
    if (this.state.files1[0] !== undefined) {
      let file1 = this.state.files1[0];
      let file2 = this.state.files2[0];
      let matches1 = this.state.leftMatches;
      let matches2 = this.state.rightMatches;
      return (
        <div>
          <div className="compare-exit">
            <Link className="badge badge-primary" role="button"
                  to={"/analyzes/" + this.state.analysisId}>Back</Link>
          </div>
          <div className="compare-names">
            <div>Student {this.state.leftName}, created at {this.state.leftTime}</div>
            <div className="custom-control custom-switch d-inline-flex">
              <div><b>{this.state.percentage + "%"}</b></div>
            </div>
            <div>Student {this.state.rightName}, created at {this.state.rightTime}</div>
          </div>
          <div className="compare-wrapper">
            <div id="left-compare" className="compare-side-wrapper" onScroll={this.handleScroll}>
              {times(file1.lines.length + 1).map((index) =>
                <div className="compare-line-wrapper" key={index}>
                  <pre className="compare">
                    <div id={"left" + index} className={this.getLineClass(matches1, index)}
                         to={this.getHrefToLine("right", index)} onClick={(event) => this.scrollTo(event)}>
                      {checkLine(file1.lines[index - 1])}
                    </div>
                  </pre>
                  <pre className="compare compare__indexes"><div onClick={this.scrollToClicked}>{index}</div></pre>
                </div>
              )}
            </div>
            <div id="right-compare" className="compare-side-wrapper" onScroll={this.handleScroll}>
              {times(file2.lines.length + 1).map((index) =>
                <div className="compare-line-wrapper" key={index}>
                  <pre className="compare compare__indexes"><div onClick={this.scrollToClicked}>{index}</div></pre>
                  <pre className="compare">
                    <div id={"right" + index} className={this.getLineClass(matches2, index)}
                         to={this.getHrefToLine("left", index)} onClick={(event) => this.scrollTo(event)}>
                      {checkLine(file2.lines[index - 1])}
                    </div>
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      );
    } else {
      return null;
    }
  }
}