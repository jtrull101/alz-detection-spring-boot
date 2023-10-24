import { Component } from '@angular/core';
import { FormBuilder } from '@angular/forms';

@Component({
  selector: 'app-model-id-prompt',
  templateUrl: './model-id-prompt.component.html',
  styleUrls: ['./model-id-prompt.component.css']
})
export class ModelIdPromptComponent {
  modelId:number|undefined;

  constructor(private formBuilder:FormBuilder) {}

  idPrompt = this.formBuilder.group({id: ''})


}
