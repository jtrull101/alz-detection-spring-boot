import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-model-delete-all',
  templateUrl: './model-delete-all.component.html',
  styleUrls: ['./model-delete-all.component.css']
})
export class ModelDeleteAllComponent {
  title = 'Result of Deleting All Models:'
  response:boolean | undefined;

  constructor(private service:ModelService){
  }

  ngOnInit() {
    this.deleteAllModels();
  }

  deleteAllModels() {
    this.service.deleteAll().subscribe(
      (response: boolean) => {
        this.response = response;
      },
      (error:HttpErrorResponse) => {
        this.response = false;
        console.log(error.error)
      }
    );
  }
}
