
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { Model } from 'src/app/models/model.model';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-load-model',
  templateUrl: './load-model.component.html',
  styleUrls: ['./load-model.component.css']
})
export class LoadModelComponent {
  fileName = '';
  file: File | undefined;
  title = 'Load archived Tensorflow model'
  model: Model | undefined;

  constructor(private service: ModelService){}

  onFilechange(event:any) {
    this.file = event.target.files[0];
  }

  upload() {
    if (this.file) {
      this.fileName = this.file.name;
      const formData = new FormData();
      formData.append("file", this.file);
      this.service.load(formData).subscribe(
        (response: Model) => {
          this.model = response;
        },
        (error:HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
