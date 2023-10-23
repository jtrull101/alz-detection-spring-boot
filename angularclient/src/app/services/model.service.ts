import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Model } from '../models/model.model';

const baseUrl = 'http://localhost:8080/api/v1/model';

@Injectable({
  providedIn: 'root'
})
export class ModelService {

  constructor(private http: HttpClient) { }

  getAll(): Observable<Model[]> {
    return this.http.get<Model[]>(`${baseUrl}/all`);
  }

  get(id:any): Observable<Model> {
    return this.http.get(`${baseUrl}?id=${id}`);
  }

  load(file:any): Observable<any> {
    return this.http.post(`${baseUrl}/load`, file);
  }

  delete(id:any): Observable<any> {
    console.log(`sending delete to: ${baseUrl}/delete?id=${id}`)
    return this.http.delete(`${baseUrl}/delete?id=${id}`);
  }

  deleteAll(): Observable<any> {
    return this.http.delete(`${baseUrl}/delete/all`);
  }

}
