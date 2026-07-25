import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { UserApiService } from '../../../core/services/user-api.service';
import { ALL_ROLES, UserResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-user-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserListComponent implements OnInit {
  private readonly usersApi = inject(UserApiService);

  readonly users = signal<UserResponse[]>([]);
  readonly roles = ALL_ROLES;
  readonly username = new FormControl('', { nonNullable: true });
  readonly name = new FormControl('', { nonNullable: true });
  readonly email = new FormControl('', { nonNullable: true });
  readonly role = new FormControl('', { nonNullable: true });
  readonly active = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    merge(
      this.username.valueChanges,
      this.name.valueChanges,
      this.email.valueChanges,
      this.role.valueChanges,
      this.active.valueChanges,
    )
      .pipe(debounceTime(300))
      .subscribe(() => this.load());
  }

  clearFilters(): void {
    this.username.setValue('');
    this.name.setValue('');
    this.email.setValue('');
    this.role.setValue('');
    this.active.setValue('');
  }

  load(): void {
    this.usersApi
      .list({
        username: this.username.value,
        name: this.name.value,
        email: this.email.value,
        role: this.role.value,
        active: this.active.value === '' ? null : this.active.value === 'true',
      })
      .subscribe({ next: (page) => this.users.set(page.content) });
  }

  activate(user: UserResponse): void {
    this.usersApi.activate(user.id).subscribe({ next: () => this.load() });
  }

  deactivate(user: UserResponse): void {
    this.usersApi.deactivate(user.id).subscribe({ next: () => this.load() });
  }

  resetPassword(user: UserResponse): void {
    const password = window.prompt(`New password for ${user.username}`);
    if (!password || password.length < 8) {
      return;
    }
    this.usersApi.resetPassword(user.id, password).subscribe();
  }
}
