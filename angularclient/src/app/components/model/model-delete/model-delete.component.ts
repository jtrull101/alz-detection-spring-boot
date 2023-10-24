import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Model } from 'src/app/models/model.model';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-model-delete',
  templateUrl: './model-delete.component.html',
  styleUrls: ['./model-delete.component.css']
})
export class ModelDeleteComponent {
  title = "Confirmation if Delete of Model Was Successful";
  id: any | undefined;
  response:boolean | undefined;

  constructor(private service:ModelService, private route:ActivatedRoute){
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.id = params['id'];
      if (this.id == undefined) {
        this.id = this.service.getModelId();
      }
      this.deleteModel();
      // reset model to 1 after delete
      this.service.setModelId(1);
    });
  }

  deleteModel() {
    this.service.delete(this.id).subscribe(
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
