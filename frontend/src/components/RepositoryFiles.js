import React from "react";
import {Link} from "react-router-dom";
import * as PROP from "../properties";
import axios from "axios";
import {Header} from "./Header";

export class RepositoryFiles extends React.Component {

  state = {
    repoId: 0,
    bases: [],
    solutions: [],
    repoName: "",
    sortedByName: ""
  };

  constructor(props, context) {
    super(props, context);
    this.state.repoId = props.match.params.id;
    axios.get(PROP.serverUrl + "/api/repositories/" + this.state.repoId).then((response) => {
      this.setState({repoName: response.data.name});
    });
    this.fetchFiles();
    this.handleChange = this.handleChange.bind(this);
    this.fetchFiles = this.fetchFiles.bind(this);
    this.deleteBaseFiles = this.deleteBaseFiles.bind(this);
    this.deleteSolutionFiles = this.deleteSolutionFiles.bind(this);
  }

  static fileElementBase(base, file) {
    return {id: file.id, branch: base.branch, updated: base.updated.toString().replace("T", " "), name: file.name};
  }

  static fileElementSolution(solution, student, file) {
    return {
      id: file.id,
      branch: solution.sourceBranch,
      student: student.student,
      updated: student.updated.replace("T", " "),
      name: file.name
    };
  }

  static filterBase(regStr, base) {
    try {
      let regexp = new RegExp(regStr);
      return regexp.test(base.name.toLowerCase()) || regexp.test(base.branch.toLowerCase()) || regexp.test(base.updated.toLowerCase());
    } catch (e) {
      return true;
    }
  }

  static baseElement(base) {
    return base.files.map((file) => RepositoryFiles.fileElementBase(base, file));
  }

  static solutionElement(solution) {
    return solution.students.flatMap((student) => RepositoryFiles.solutionUserElement(solution, student));
  }

  static solutionUserElement(solution, student) {
    return student.files.map((file) => RepositoryFiles.fileElementSolution(solution, student, file));
  }

  deleteBaseFiles() {
    let selectedFiles = this.state.bases.filter(
      (it) => RepositoryFiles.filterBase(this.state.sortedByName, it));
    axios.post(PROP.serverUrl + "/api/repositories/" + this.state.repoId + "/bases/delete",
      selectedFiles.map((file) => file.id)
    );
    this.setState({
      bases: this.state.bases.filter((it) => selectedFiles.indexOf(it) < 0),
      sortedByName: ""
    });
  }

  deleteSolutionFiles() {
    let selectedFiles = this.state.solutions.filter(
      (it) => RepositoryFiles.filterSolution(this.state.sortedByName, it));
    axios.post(PROP.serverUrl + "/api/repositories/" + this.state.repoId + "/solutions/delete",
      selectedFiles.map((file) => file.id)
    );
    this.setState({
      bases: this.state.solutions.filter((it) => selectedFiles.indexOf(it) < 0),
      sortedByName: ""
    });
  }

  handleChange(event) {
    this.setState({
      [event.target.name]: event.target.value.toLowerCase()
    });
  }

  static filterSolution(regStr, sol) {
    try {
      let regexp = new RegExp(regStr);
      return regexp.test(sol.name.toLowerCase()) || regexp.test(sol.branch.toLowerCase()) || regexp.test(sol.updated.toLowerCase()) || regexp.test(sol.student.toLowerCase());
    } catch (e) {
      return true;
    }
  }

  fetchFiles() {
    axios.get(PROP.serverUrl + "/api/repositories/" + this.state.repoId + "/files").then((response) => {
      let bases = response.data.bases.flatMap((base) => RepositoryFiles.baseElement(base));
      let solutions = response.data.solutions.flatMap((solution) => RepositoryFiles.solutionElement(solution));
      this.setState({
        bases, solutions
      });
    });
  }

  render() {
    return (<div className="container">
      {Header(
        <ol className="breadcrumb">
          <li className="breadcrumb-item"><Link to="/repos">Repositories</Link></li>
          <li className="breadcrumb-item"><Link to={"/repos/" + this.state.repoId}>{this.state.repoName}</Link></li>
          <li className="breadcrumb-item"><Link onClick={() => this.fetchFiles()}
                                                to={"/repos/" + this.state.repoId + "/files"}>Files</Link></li>
        </ol>)}

        <div className="row container">
                <div className="row mb-2">
                  <h5>Downloaded files of repository {this.state.repoName}</h5>
                </div>
        <div className="col container">
            <div className="row">
              <h6>Base files</h6>
              <table className="table table-hover">
                <thead className="thead-light">
                <tr>
                  <th className="break-word">Name</th>
                  <th>Branch</th>
                  <th>Updated</th>
                </tr>
                </thead>
                <tbody>
                {this.state.bases.filter(
                  (it) => RepositoryFiles.filterBase(this.state.sortedByName, it)
                ).map((base) => <tr>
                  <td className="break-word">{base.name}</td>
                  <td className="break-word">{base.branch}</td>
                  <td className="th-lg text-nowrap">{base.updated}</td>
                </tr>)}
                </tbody>
              </table>
            </div>
            </div>
              <div className="col list-group input-group col-sm-3">
                <input name="sortedByName" onChange={this.handleChange} autoComplete="off"
                       className="list-group-item list-group-item-action text-input" placeholder="Full-text search"/>
              </div>
            </div>



          <div className="container table-responsive">
              <h6>Solution files</h6>
              <table className="table table-hover">
                <thead className="thead-light">
                <tr>
                  <th>Student</th>
                  <th className="break-word">Name</th>
                  <th className="break-word">Branch</th>
                  <th className="th-lg text-nowrap">Updated</th>
                </tr>
                </thead>
                <tbody>
                {this.state.solutions.filter(
                  (it) => RepositoryFiles.filterSolution(this.state.sortedByName, it)
                ).map((base) => <tr>
                  <td>{base.student}</td>
                  <td className="break-word">{base.name}</td>
                  <td className="break-word">{base.branch}</td>
                  <td className="th-lg text-nowrap">{base.updated}</td>
                </tr>)}
                </tbody>
              </table>
          </div>

    </div>);
  }
}