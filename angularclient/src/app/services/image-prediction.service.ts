import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ImagePrediction } from '../models/image-prediction.model';
import { Observable } from 'rxjs';



@Injectable({
  providedIn: 'root'
})
export class ImagePredictionService {



  constructor(private http: HttpClient) { }

  predictFromFile(file:any, modelId:number): Observable<ImagePrediction> {
    return this.http.post(`${this.getBaseUrl(modelId)}`, file);
  }

  predictFromRandom(modelId:number): Observable<ImagePrediction> {
    return this.http.get(`${this.getBaseUrl(modelId)}/random`);
  }

  predictFromRandomCategory(modelId:number, impairment:any): Observable<ImagePrediction> {
    return this.http.get(`${this.getBaseUrl(modelId)}/random/${impairment}`);
  }

  getPrediction(id:any, modelId:number): Observable<ImagePrediction> {
    return this.http.get(`${this.getBaseUrl(modelId)}?id=${id}`);
  }

  deletePrediction(id:any, modelId:number): Observable<any> {
    return this.http.delete(`${this.getBaseUrl(modelId)}/delete?id=${id}`);
  }

  getBaseUrl(modelId:number): String {
    return `http://localhost:8080/api/v1/model/${modelId}/predict`;
  }
}
