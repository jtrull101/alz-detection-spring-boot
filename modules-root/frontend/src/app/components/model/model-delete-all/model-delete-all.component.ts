import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-model-delete-all',
  templateUrl: './model-delete-all.component.html',
  styleUrls: ['./model-delete-all.component.css']
})
export class ModelDeleteAllComponent {
  title = 'Delete All Models'
  response:boolean | undefined;

  constructor(private service:ModelService){
  }

  deleteAllModels() {
    this.service.deleteAll().subscribe(
      (response: boolean) => {
        this.response = response;
        this.service.setModelId(1);
      },
      (error:HttpErrorResponse) => {
        this.response = false;
        console.log(error.error)
      }
    );
  }
}
