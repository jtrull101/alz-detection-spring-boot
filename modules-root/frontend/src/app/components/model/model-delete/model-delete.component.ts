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
  title = "Delete Single Model";
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
    });
  }

  deleteModel() {
    this.service.delete(this.id).subscribe(
      (response: boolean) => {
        this.response = response;
        this.service.setModelId(1);
      },
      (error:any) => {
        this.response = false;
        console.log(error.error)
      }
    );
  }
}
