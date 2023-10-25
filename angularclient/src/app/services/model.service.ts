import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Model } from '../models/model.model';

const baseUrl = 'http://localhost:8080/api/v1/model';

@Injectable({
  providedIn: 'root'
})
export class ModelService {
  private modelId:number=1;

  constructor(private http: HttpClient) { }

  getAll(): Observable<Model[]> {
    return this.http.get<Model[]>(`${baseUrl}/all`);
  }

  get(id:any): Observable<Model> {
    console.log(`${baseUrl}?id=${id}`)
    return this.http.get(`${baseUrl}?id=${id}`);
  }

  load(file:any): Observable<any> {
    return this.http.post(`${baseUrl}/load`, file);
  }

  delete(id:any): Observable<any> {
    return this.http.delete(`${baseUrl}/delete?id=${id}`);
  }

  deleteAll(): Observable<any> {
    return this.http.delete(`${baseUrl}/delete/all`);
  }

  /**
   *  Note: These methods are clearly not threadsafe. If we wanted to ensure a User could select a Model for their predictions and only their predictions, we would need to
   *    create Users in the database and store their associated Models and Predictions
   */
  getModelId(): number|undefined {
    return this.modelId;
  }

  setModelId(modelId:number): void {
    this.modelId = modelId;
  }
}
