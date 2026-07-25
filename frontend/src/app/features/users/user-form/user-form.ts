import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ALL_ROLES } from '../../../core/models/api.models';
import { UserApiService } from '../../../core/services/user-api.service';

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss',
})
export class UserFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly usersApi = inject(UserApiService);

  readonly roles = ALL_ROLES;
  readonly isEdit = signal(false);
  readonly userId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.minLength(8)]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    roles: [[] as string[], Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      const id = Number(idParam);
      this.isEdit.set(true);
      this.userId.set(id);
      this.form.controls.username.disable();
      this.form.controls.password.clearValidators();
      this.form.controls.password.updateValueAndValidity();
      this.usersApi.get(id).subscribe({
        next: (user) => {
          this.form.patchValue({
            username: user.username,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            phone: user.phone ?? '',
            roles: user.roles,
          });
        },
      });
    } else {
      this.form.controls.password.addValidators(Validators.required);
      this.form.controls.password.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    const raw = this.form.getRawValue();

    if (this.isEdit()) {
      this.usersApi
        .update(this.userId()!, {
          email: raw.email,
          firstName: raw.firstName,
          lastName: raw.lastName,
          phone: raw.phone || null,
          roles: raw.roles,
        })
        .subscribe({
          next: () => void this.router.navigate(['/users']),
          error: () => this.error.set('Unable to update user'),
        });
      return;
    }

    this.usersApi
      .create({
        username: raw.username,
        email: raw.email,
        password: raw.password,
        firstName: raw.firstName,
        lastName: raw.lastName,
        phone: raw.phone || null,
        roles: raw.roles,
      })
      .subscribe({
        next: () => void this.router.navigate(['/users']),
        error: () => this.error.set('Unable to create user'),
      });
  }
}
