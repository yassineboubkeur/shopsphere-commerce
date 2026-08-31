import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminUserRow {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getUsers(): Observable<AdminUserRow[]> {
    return this.http.get<AdminUserRow[]>(`${environment.apiUrl}/api/admin/users`);
  }

  updateRole(id: number, role: string): Observable<unknown> {
    return this.http.put(`${environment.apiUrl}/api/admin/users/${id}/role`, { role });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/admin/users/${id}`);
  }
}