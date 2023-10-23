import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Model } from 'src/app/models/model.model';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-model-list',
  templateUrl: './model-list.component.html',
  styleUrls: ['./model-list.component.css']
})
export class ModelListComponent implements OnInit {
  title = "All Available Model IDs";
  public models: Model[] | undefined;

  constructor(private service: ModelService) {}

  ngOnInit(): void {
    this.getModels();
  }

  public getModels(): void {
    this.service.getAll().subscribe(
      (response: Model[]) => {
        this.models = response;
      },
      (error:HttpErrorResponse) => {
        alert(error.message);
      }
    )
  }
}
