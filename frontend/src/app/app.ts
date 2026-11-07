import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { API_BASE_URL } from './runtime-config';

interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  firmName: string;
  firmRole: FirmRole;
}

type FirmRole = 'OWNER' | 'ADMIN' | 'STAFF' | 'VIEWER';

interface FirmMember {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: FirmRole;
  joinedAt: string;
}

interface FirmInvitation {
  id: string;
  email: string;
  role: FirmRole;
  createdAt: string;
  expiresAt: string;
}

interface FirmTeam {
  firmName: string;
  currentRole: FirmRole;
  members: FirmMember[];
  invitations: FirmInvitation[];
}

interface FirmInvitationPreview {
  firmName: string;
  email: string;
  role: FirmRole;
  expiresAt: string;
}

interface CreatedFirmInvitation {
  id: string;
  email: string;
  role: FirmRole;
  token: string;
  expiresAt: string;
}

interface Project {
  id: string;
  name: string;
  description: string | null;
  repositoryPath: string | null;
  createdAt: string;
}

type OnboardingDocumentStatus = 'REQUESTED' | 'RECEIVED' | 'APPROVED' | 'REJECTED';

interface OnboardingDocument {
  id: string;
  name: string;
  status: OnboardingDocumentStatus;
  createdAt: string;
}

interface AccountingClient {
  id: string;
  legalName: string;
  contactName: string;
  contactEmail: string | null;
  contactPhone: string | null;
  onboardingStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETE';
  documentCount: number;
  approvedDocumentCount: number;
  documents: OnboardingDocument[];
  createdAt: string;
}

interface OnboardingTemplate {
  id: string;
  name: string;
  documentNames: string[];
  createdAt: string;
}

interface SalesCustomer {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  createdAt: string;
}

interface SalesInvoicePayment {
  id: string;
  amount: number;
  paymentDate: string;
  note: string | null;
  createdAt: string;
}

type SalesInvoiceStatus = 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';

interface SalesInvoice {
  id: string;
  invoiceNumber: string;
  customerId: string;
  customerName: string;
  description: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  amountPaid: number;
  balance: number;
  status: SalesInvoiceStatus;
  payments: SalesInvoicePayment[];
  createdAt: string;
}

type PurchaseBillStatus = 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';

interface PurchaseBillPayment {
  id: string;
  amount: number;
  paymentDate: string;
  note: string | null;
  createdAt: string;
}

interface PurchaseBill {
  id: string;
  supplierName: string;
  billNumber: string;
  category: string;
  description: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  amountPaid: number;
  balance: number;
  status: PurchaseBillStatus;
  payments: PurchaseBillPayment[];
  createdAt: string;
}

interface Workflow {
  id: string;
  projectId: string;
  name: string;
  status: 'PLANNED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
}

interface WorkflowRun {
  id: string;
  workflowId: string;
  startedBy: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  completedAt: string | null;
  completedTaskCount: number;
  failureReason: string | null;
}

interface AiUsage {
  period: string;
  monthlyLimitUsd: number;
  spentUsd: number;
  remainingUsd: number;
  promptTokens: number;
  completionTokens: number;
}

interface Task {
  id: string;
  workflowId: string;
  title: string;
  description: string | null;
  result: string | null;
  agentRole: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED';
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvalNote: string | null;
  executionAttempts: number;
  lastExecutionFailure: string | null;
  createdAt: string;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly api = API_BASE_URL;
  private readonly tokenKey = 'ai-business-platform-token';

  authMode: 'login' | 'register' = 'login';
  token = sessionStorage.getItem(this.tokenKey);
  user: User | null = null;
  firmTeam: FirmTeam | null = null;
  invitePreview: FirmInvitationPreview | null = null;
  invitationToken = '';
  createdInvitationUrl = '';
  inviteEmail = '';
  inviteRole: FirmRole = 'STAFF';
  memberRoleSelections: Record<string, FirmRole> = {};
  accountingClients: AccountingClient[] = [];
  onboardingTemplates: OnboardingTemplate[] = [];
  salesCustomers: SalesCustomer[] = [];
  salesInvoices: SalesInvoice[] = [];
  purchaseBills: PurchaseBill[] = [];
  selectedAccountingClientId = '';
  selectedClientTemplateId = '';
  editingTemplateId = '';
  templateName = '';
  templateDocumentNames = '';
  projects: Project[] = [];
  workflows: Workflow[] = [];
  workflowRuns: WorkflowRun[] = [];
  aiUsage: AiUsage | null = null;
  aiUsageLoading = false;
  aiUsageError = '';
  tasks: Task[] = [];
  selectedProjectId = '';
  selectedWorkflowId = '';
  busy = false;
  errorMessage = '';
  successMessage = '';

  email = '';
  password = '';
  firstName = '';
  lastName = '';
  firmName = '';

  projectName = '';
  projectDescription = '';
  repositoryPath = '';
  accountingClientLegalName = '';
  accountingClientContactName = '';
  accountingClientContactEmail = '';
  accountingClientContactPhone = '';
  customOnboardingDocumentName = '';
  salesCustomerName = '';
  salesCustomerEmail = '';
  salesCustomerPhone = '';
  salesInvoiceNumber = '';
  salesInvoiceCustomerId = '';
  salesInvoiceDescription = '';
  salesInvoiceIssueDate = this.localDateString();
  salesInvoiceDueDate = this.localDateString(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));
  salesInvoiceCurrency = 'USD';
  salesInvoiceSubtotal = '';
  salesInvoiceTaxRate = '0';
  paymentAmounts: Record<string, string> = {};
  paymentDate = this.localDateString();
  purchaseSupplierName = '';
  purchaseBillNumber = '';
  purchaseBillCategory = '';
  purchaseBillDescription = '';
  purchaseBillIssueDate = this.localDateString();
  purchaseBillDueDate = this.localDateString(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));
  purchaseBillCurrency = 'USD';
  purchaseBillSubtotal = '';
  purchaseBillTaxRate = '0';
  billPaymentAmounts: Record<string, string> = {};
  billPaymentDate = this.localDateString();
  workflowName = '';
  taskTitle = '';
  taskDescription = '';
  agentRole = 'DEVELOPER';

  readonly agentRoles = [
    'PRODUCT',
    'ARCHITECT',
    'PROJECT_MANAGER',
    'DEVELOPER',
    'QA',
    'SECURITY',
    'DEVOPS',
  ];

  ngOnInit(): void {
    this.invitationToken = new URLSearchParams(window.location.search).get('invite') ?? '';
    if (this.invitationToken) {
      this.authMode = 'register';
      this.loadInvitationPreview();
    } else if (this.token) {
      this.loadCurrentUser();
    }
  }

  get canManageFirm(): boolean {
    return this.user?.firmRole === 'OWNER' || this.user?.firmRole === 'ADMIN';
  }

  get canAssignAdmin(): boolean {
    return this.user?.firmRole === 'OWNER';
  }

  get isReadOnly(): boolean {
    return this.user?.firmRole === 'VIEWER';
  }

  canManageMember(member: FirmMember): boolean {
    if (!this.canManageFirm || member.role === 'OWNER' || member.id === this.user?.id) {
      return false;
    }
    return this.canAssignAdmin || member.role !== 'ADMIN';
  }

  get selectedProject(): Project | undefined {
    return this.projects.find((project) => project.id === this.selectedProjectId);
  }

  get selectedWorkflow(): Workflow | undefined {
    return this.workflows.find((workflow) => workflow.id === this.selectedWorkflowId);
  }

  get selectedAccountingClient(): AccountingClient | undefined {
    return this.accountingClients.find((client) => client.id === this.selectedAccountingClientId);
  }

  get requestedOnboardingDocuments(): number {
    return this.accountingClients
      .flatMap((client) => client.documents)
      .filter((document) => document.status === 'REQUESTED').length;
  }

  get receivedOnboardingDocuments(): number {
    return this.accountingClients
      .flatMap((client) => client.documents)
      .filter((document) => document.status === 'RECEIVED').length;
  }

  get completedClientOnboardings(): number {
    return this.accountingClients.filter((client) => client.onboardingStatus === 'COMPLETE').length;
  }

  get salesInvoiceDatesValid(): boolean {
    return this.salesInvoiceDueDate >= this.salesInvoiceIssueDate;
  }

  get purchaseBillDatesValid(): boolean {
    return this.purchaseBillDueDate >= this.purchaseBillIssueDate;
  }

  get completedTasks(): number {
    return this.tasks.filter((task) => task.status === 'DONE').length;
  }

  get progressPercent(): number {
    return this.tasks.length ? Math.round((this.completedTasks / this.tasks.length) * 100) : 0;
  }

  get approvedTasks(): number {
    return this.tasks.filter((task) => task.approvalStatus === 'APPROVED').length;
  }

  get aiUsagePercent(): number {
    if (!this.aiUsage || this.aiUsage.monthlyLimitUsd <= 0) {
      return 0;
    }
    return Math.min((this.aiUsage.spentUsd / this.aiUsage.monthlyLimitUsd) * 100, 100);
  }

  submitAuth(): void {
    this.clearMessages();
    this.busy = true;
    const request = this.authMode === 'login'
      ? this.http.post<AuthResponse>(`${this.api}/auth/login`, {
          email: this.email,
          password: this.password,
        })
      : this.http.post<AuthResponse>(`${this.api}/auth/register`, {
          email: this.email,
          password: this.password,
          firstName: this.firstName,
          lastName: this.lastName,
          firmName: this.firmName || null,
          invitationToken: this.invitationToken || null,
        });

    request.pipe(finalize(() => {
      this.busy = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (response) => {
        this.token = response.token;
        sessionStorage.setItem(this.tokenKey, response.token);
        this.user = response.user;
        this.password = '';
        this.invitationToken = '';
        this.invitePreview = null;
        window.history.replaceState(null, '', window.location.pathname);
        this.loadProjects();
        this.loadFirmTeam();
        this.loadAccountingClients();
        this.loadOnboardingTemplates();
        this.loadSalesData();
        this.loadPurchaseBills();
        this.loadAiUsage();
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.showError(error, 'Could not sign in. Check your details and try again.');
        this.changeDetector.markForCheck();
      },
    });
  }

  logout(): void {
    sessionStorage.removeItem(this.tokenKey);
    this.token = null;
    this.user = null;
    this.firmTeam = null;
    this.accountingClients = [];
    this.onboardingTemplates = [];
    this.salesCustomers = [];
    this.salesInvoices = [];
    this.paymentAmounts = {};
    this.purchaseBills = [];
    this.billPaymentAmounts = {};
    this.salesCustomerName = '';
    this.salesCustomerEmail = '';
    this.salesCustomerPhone = '';
    this.salesInvoiceNumber = '';
    this.salesInvoiceCustomerId = '';
    this.salesInvoiceDescription = '';
    this.salesInvoiceIssueDate = this.localDateString();
    this.salesInvoiceDueDate = this.localDateString(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));
    this.salesInvoiceCurrency = 'USD';
    this.salesInvoiceSubtotal = '';
    this.salesInvoiceTaxRate = '0';
    this.paymentDate = this.localDateString();
    this.purchaseSupplierName = '';
    this.purchaseBillNumber = '';
    this.purchaseBillCategory = '';
    this.purchaseBillDescription = '';
    this.purchaseBillIssueDate = this.localDateString();
    this.purchaseBillDueDate = this.localDateString(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));
    this.purchaseBillCurrency = 'USD';
    this.purchaseBillSubtotal = '';
    this.purchaseBillTaxRate = '0';
    this.billPaymentDate = this.localDateString();
    this.selectedAccountingClientId = '';
    this.selectedClientTemplateId = '';
    this.resetTemplateEditor();
    this.createdInvitationUrl = '';
    this.memberRoleSelections = {};
    this.projects = [];
    this.workflows = [];
    this.workflowRuns = [];
    this.aiUsage = null;
    this.aiUsageError = '';
    this.tasks = [];
    this.selectedProjectId = '';
    this.selectedWorkflowId = '';
    this.clearMessages();
  }

  createProject(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<Project>(`${this.api}/projects`, {
        name: this.projectName,
        description: this.projectDescription || null,
        repositoryPath: this.repositoryPath || null,
      }),
      (project) => {
        this.projectName = '';
        this.projectDescription = '';
        this.repositoryPath = '';
        this.projects = [project, ...this.projects];
        this.selectProject(project.id);
        this.successMessage = 'Project created.';
      },
      'Could not create the project.',
    );
  }

  createAccountingClient(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<AccountingClient>(`${this.api}/clients`, {
        legalName: this.accountingClientLegalName,
        contactName: this.accountingClientContactName,
        contactEmail: this.accountingClientContactEmail || null,
        contactPhone: this.accountingClientContactPhone || null,
        templateId: this.selectedClientTemplateId || null,
      }),
      (client) => {
        this.accountingClients = [client, ...this.accountingClients];
        this.selectedAccountingClientId = client.id;
        this.accountingClientLegalName = '';
        this.accountingClientContactName = '';
        this.accountingClientContactEmail = '';
        this.accountingClientContactPhone = '';
        this.selectedClientTemplateId = '';
        this.successMessage = 'Accounting client created with a starter onboarding checklist.';
      },
      'Could not create the accounting client.',
    );
  }

  createSalesCustomer(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<SalesCustomer>(`${this.api}/sales/customers`, {
        name: this.salesCustomerName,
        email: this.salesCustomerEmail || null,
        phone: this.salesCustomerPhone || null,
      }),
      (customer) => {
        this.salesCustomers = [...this.salesCustomers, customer]
          .sort((left, right) => left.name.localeCompare(right.name));
        this.salesInvoiceCustomerId = customer.id;
        this.salesCustomerName = '';
        this.salesCustomerEmail = '';
        this.salesCustomerPhone = '';
        this.successMessage = 'Sales customer created.';
      },
      'Could not create the sales customer.',
    );
  }

  createSalesInvoice(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<SalesInvoice>(`${this.api}/sales/invoices`, {
        invoiceNumber: this.salesInvoiceNumber,
        customerId: this.salesInvoiceCustomerId,
        description: this.salesInvoiceDescription,
        issueDate: this.salesInvoiceIssueDate,
        dueDate: this.salesInvoiceDueDate,
        currency: this.salesInvoiceCurrency,
        subtotal: this.salesInvoiceSubtotal,
        taxRate: this.salesInvoiceTaxRate,
      }),
      (invoice) => {
        this.salesInvoices = [invoice, ...this.salesInvoices];
        this.salesInvoiceNumber = '';
        this.salesInvoiceDescription = '';
        this.salesInvoiceSubtotal = '';
        this.salesInvoiceTaxRate = '0';
        this.successMessage = `Invoice ${invoice.invoiceNumber} issued.`;
      },
      'Could not create the invoice.',
    );
  }

  recordSalesInvoicePayment(invoice: SalesInvoice): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<SalesInvoice>(`${this.api}/sales/invoices/${invoice.id}/payments`, {
        amount: this.paymentAmounts[invoice.id],
        paymentDate: this.paymentDate,
      }),
      (updated) => {
        this.replaceSalesInvoice(updated);
        delete this.paymentAmounts[invoice.id];
        this.successMessage = `Payment recorded for invoice ${invoice.invoiceNumber}.`;
      },
      'Could not record the invoice payment.',
    );
  }

  salesInvoiceStatusClass(status: SalesInvoiceStatus): string {
    return status.toLowerCase().replace('_', '-');
  }

  createPurchaseBill(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<PurchaseBill>(`${this.api}/purchases/bills`, {
        supplierName: this.purchaseSupplierName,
        billNumber: this.purchaseBillNumber,
        category: this.purchaseBillCategory,
        description: this.purchaseBillDescription,
        issueDate: this.purchaseBillIssueDate,
        dueDate: this.purchaseBillDueDate,
        currency: this.purchaseBillCurrency,
        subtotal: this.purchaseBillSubtotal,
        taxRate: this.purchaseBillTaxRate,
      }),
      (bill) => {
        this.purchaseBills = [bill, ...this.purchaseBills];
        this.purchaseBillNumber = '';
        this.purchaseBillCategory = '';
        this.purchaseBillDescription = '';
        this.purchaseBillSubtotal = '';
        this.purchaseBillTaxRate = '0';
        this.successMessage = `Supplier bill ${bill.billNumber} recorded.`;
      },
      'Could not record the supplier bill.',
    );
  }

  recordPurchaseBillPayment(bill: PurchaseBill): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<PurchaseBill>(
        `${this.api}/purchases/bills/${bill.id}/payments`,
        {
          amount: this.billPaymentAmounts[bill.id],
          paymentDate: this.billPaymentDate,
        },
      ),
      (updated) => {
        this.replacePurchaseBill(updated);
        delete this.billPaymentAmounts[bill.id];
        this.successMessage = `Payment recorded for bill ${bill.billNumber}.`;
      },
      'Could not record the supplier bill payment.',
    );
  }

  purchaseBillStatusClass(status: PurchaseBillStatus): string {
    return status.toLowerCase().replace('_', '-');
  }

  saveOnboardingTemplate(): void {
    const documentNames = this.templateDocumentNames
      .split(/\r?\n/)
      .map((name) => name.trim())
      .filter(Boolean);
    const request = {
      name: this.templateName,
      documentNames,
    };
    const save = this.editingTemplateId
      ? this.http.put<OnboardingTemplate>(
          `${this.api}/onboarding-templates/${this.editingTemplateId}`,
          request,
        )
      : this.http.post<OnboardingTemplate>(`${this.api}/onboarding-templates`, request);

    this.clearMessages();
    this.runRequest(
      save,
      (template) => {
        this.onboardingTemplates = [
          ...this.onboardingTemplates.filter((item) => item.id !== template.id),
          template,
        ].sort((left, right) => left.name.localeCompare(right.name));
        this.resetTemplateEditor();
        this.successMessage = 'Onboarding template saved.';
      },
      'Could not save the onboarding template.',
    );
  }

  createFirmInvitation(): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<CreatedFirmInvitation>(`${this.api}/firm/invitations`, {
        email: this.inviteEmail,
        role: this.inviteRole,
      }),
      (invitation) => {
        this.inviteEmail = '';
        this.createdInvitationUrl = `${window.location.origin}/?invite=${encodeURIComponent(invitation.token)}`;
        this.successMessage = `Invitation created for ${invitation.email}. Share this link before it expires.`;
        this.loadFirmTeam();
      },
      'Could not create the firm invitation.',
    );
  }

  copyInvitationLink(): void {
    if (!this.createdInvitationUrl || !navigator.clipboard) {
      this.errorMessage = 'Clipboard access is unavailable. Copy the invitation link manually.';
      return;
    }
    navigator.clipboard.writeText(this.createdInvitationUrl).then(
      () => {
        this.successMessage = 'Invitation link copied.';
        this.changeDetector.markForCheck();
      },
      () => {
        this.errorMessage = 'Could not copy the invitation link. Copy it manually.';
        this.changeDetector.markForCheck();
      },
    );
  }

  saveFirmMemberRole(member: FirmMember): void {
    const role = this.memberRoleSelections[member.id];
    if (!role || role === member.role) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.patch<FirmMember>(`${this.api}/firm/members/${member.id}`, { role }),
      (updated) => {
        if (this.firmTeam) {
          this.firmTeam = {
            ...this.firmTeam,
            members: this.firmTeam.members.map((item) => item.id === updated.id ? updated : item),
          };
          this.memberRoleSelections[updated.id] = updated.role;
        }
        this.successMessage = `Updated ${updated.firstName}'s firm role.`;
      },
      'Could not update the firm member role.',
    );
  }

  removeFirmMember(member: FirmMember): void {
    if (!window.confirm(`Remove ${member.firstName} ${member.lastName} from ${this.user?.firmName}? They will lose access immediately.`)) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.delete<void>(`${this.api}/firm/members/${member.id}`),
      () => {
        if (this.firmTeam) {
          this.firmTeam = {
            ...this.firmTeam,
            members: this.firmTeam.members.filter((item) => item.id !== member.id),
          };
        }
        delete this.memberRoleSelections[member.id];
        this.successMessage = `${member.firstName} was removed from the firm.`;
      },
      'Could not remove the firm member.',
    );
  }

  revokeFirmInvitation(invitation: FirmInvitation): void {
    if (!window.confirm(`Revoke the invitation for ${invitation.email}?`)) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.delete<void>(`${this.api}/firm/invitations/${invitation.id}`),
      () => {
        if (this.firmTeam) {
          this.firmTeam = {
            ...this.firmTeam,
            invitations: this.firmTeam.invitations.filter((item) => item.id !== invitation.id),
          };
        }
        this.successMessage = 'Invitation revoked.';
      },
      'Could not revoke the invitation.',
    );
  }

  editOnboardingTemplate(template: OnboardingTemplate): void {
    this.editingTemplateId = template.id;
    this.templateName = template.name;
    this.templateDocumentNames = template.documentNames.join('\n');
    this.clearMessages();
  }

  deleteOnboardingTemplate(template: OnboardingTemplate): void {
    if (!window.confirm(`Delete the "${template.name}" template? Existing client checklists will not change.`)) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.delete<void>(`${this.api}/onboarding-templates/${template.id}`),
      () => {
        this.onboardingTemplates = this.onboardingTemplates.filter((item) => item.id !== template.id);
        if (this.selectedClientTemplateId === template.id) {
          this.selectedClientTemplateId = '';
        }
        if (this.editingTemplateId === template.id) {
          this.resetTemplateEditor();
        }
        this.successMessage = 'Onboarding template deleted. Existing client checklists were not changed.';
      },
      'Could not delete the onboarding template.',
    );
  }

  resetTemplateEditor(): void {
    this.editingTemplateId = '';
    this.templateName = '';
    this.templateDocumentNames = '';
  }

  selectAccountingClient(clientId: string): void {
    this.selectedAccountingClientId = clientId;
    this.customOnboardingDocumentName = '';
    this.clearMessages();
  }

  addOnboardingDocument(): void {
    const client = this.selectedAccountingClient;
    if (!client) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.post<OnboardingDocument>(
        `${this.api}/clients/${client.id}/documents`,
        { name: this.customOnboardingDocumentName },
      ),
      (document) => {
        const documents = [...client.documents, document];
        this.replaceAccountingClient({
          ...client,
          documents,
          documentCount: documents.length,
          onboardingStatus: documents.every((item) => item.status === 'REQUESTED')
            ? 'NOT_STARTED'
            : 'IN_PROGRESS',
        });
        this.customOnboardingDocumentName = '';
        this.successMessage = 'Custom document requirement added.';
      },
      'Could not add the document requirement.',
    );
  }

  updateOnboardingDocumentStatus(document: OnboardingDocument, status: OnboardingDocumentStatus): void {
    const client = this.selectedAccountingClient;
    if (!client) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.patch<AccountingClient>(
        `${this.api}/clients/${client.id}/documents/${document.id}`,
        { status },
      ),
      (updated) => this.replaceAccountingClient(updated),
      'Could not update the document status.',
    );
  }

  selectProject(projectId: string): void {
    this.selectedProjectId = projectId;
    this.selectedWorkflowId = '';
    this.workflows = [];
    this.workflowRuns = [];
    this.tasks = [];
    this.workflowName = '';
    if (!projectId) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.get<Workflow[]>(`${this.api}/projects/${projectId}/workflows`),
      (workflows) => (this.workflows = workflows),
      'Could not load workflows.',
    );
  }

  createWorkflow(): void {
    if (!this.selectedProjectId) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.post<Workflow>(
        `${this.api}/projects/${this.selectedProjectId}/workflows`,
        { name: this.workflowName },
      ),
      (workflow) => {
        this.workflowName = '';
        this.workflows = [...this.workflows, workflow];
        this.selectWorkflow(workflow.id);
        this.successMessage = 'Workflow created.';
      },
      'Could not create the workflow.',
    );
  }

  selectWorkflow(workflowId: string): void {
    this.selectedWorkflowId = workflowId;
    this.workflowRuns = [];
    this.tasks = [];
    if (!workflowId || !this.selectedProjectId) {
      return;
    }
    this.clearMessages();
    this.loadTasks();
    this.loadWorkflowRuns();
  }

  createTask(): void {
    if (!this.selectedProjectId || !this.selectedWorkflowId) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.post<Task>(
        `${this.api}/projects/${this.selectedProjectId}/workflows/${this.selectedWorkflowId}/tasks`,
        {
          title: this.taskTitle,
          description: this.taskDescription || null,
          agentRole: this.agentRole,
        },
      ),
      (task) => {
        this.tasks = [...this.tasks, task];
        this.taskTitle = '';
        this.taskDescription = '';
        this.successMessage = 'Task created. Review and approve it before execution.';
      },
      'Could not create the task.',
    );
  }

  approveTask(task: Task): void {
    this.clearMessages();
    this.runRequest(
      this.http.patch<Task>(
        `${this.taskPath(task)}/approval`,
        { status: 'APPROVED', note: 'Approved in dashboard' },
      ),
      (updated) => this.replaceTask(updated),
      'Could not approve the task.',
    );
  }

  executeTask(task: Task): void {
    this.clearMessages();
    this.runRequest(
      this.http.post<Task>(`${this.taskPath(task)}/execute`, {}),
      (updated) => {
        this.replaceTask(updated);
        this.successMessage = `Task "${updated.title}" finished with status ${updated.status}.`;
      },
      'Task execution failed.',
    );
  }

  executeWorkflow(): void {
    if (!this.selectedProjectId || !this.selectedWorkflowId) {
      return;
    }
    this.clearMessages();
    this.runRequest(
      this.http.post<Workflow>(
        `${this.api}/projects/${this.selectedProjectId}/workflows/${this.selectedWorkflowId}/execute`,
        {},
      ),
      (workflow) => {
        this.replaceWorkflow(workflow);
        this.loadTasks();
        this.loadWorkflowRuns();
        this.successMessage = `Workflow finished with status ${workflow.status}.`;
      },
      'Workflow could not be executed. Check that every task is approved and ready.',
    );
  }

  refresh(): void {
    this.clearMessages();
    this.loadAccountingClients();
    this.loadOnboardingTemplates();
    this.loadSalesData();
    this.loadPurchaseBills();
    this.loadFirmTeam();
    this.loadAiUsage();
    const projectId = this.selectedProjectId;
    const workflowId = this.selectedWorkflowId;
    this.runRequest(
      this.http.get<Project[]>(`${this.api}/projects`),
      (projects) => {
        this.projects = projects;
        if (!projects.some((project) => project.id === projectId)) {
          this.selectedProjectId = '';
          this.selectedWorkflowId = '';
          this.workflows = [];
          this.workflowRuns = [];
          this.tasks = [];
          return;
        }
        this.runRequest(
          this.http.get<Workflow[]>(`${this.api}/projects/${projectId}/workflows`),
          (workflows) => {
            this.workflows = workflows;
            if (workflows.some((workflow) => workflow.id === workflowId)) {
              this.selectedWorkflowId = workflowId;
              this.loadTasks();
            } else {
              this.selectedWorkflowId = '';
              this.workflowRuns = [];
              this.tasks = [];
            }
          },
          'Could not load workflows.',
        );
      },
      'Could not load projects.',
    );
  }

  private loadCurrentUser(): void {
    this.http.get<User>(`${this.api}/auth/me`).subscribe({
      next: (user) => {
        this.user = user;
        this.loadProjects();
        this.loadFirmTeam();
        this.loadAccountingClients();
        this.loadOnboardingTemplates();
        this.loadSalesData();
        this.loadPurchaseBills();
        this.loadAiUsage();
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.logout();
        } else {
          this.showError(error, 'Could not load your account.');
        }
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadProjects(): void {
    this.runRequest(
      this.http.get<Project[]>(`${this.api}/projects`),
      (projects) => {
        this.projects = projects;
        if (this.selectedProjectId && projects.some((project) => project.id === this.selectedProjectId)) {
          this.selectProject(this.selectedProjectId);
        }
      },
      'Could not load projects.',
    );
  }

  private loadFirmTeam(): void {
    this.runRequest(
      this.http.get<FirmTeam>(`${this.api}/firm/team`),
      (team) => {
        this.firmTeam = team;
        this.memberRoleSelections = Object.fromEntries(
          team.members.map((member) => [member.id, member.role]),
        );
      },
      'Could not load the firm team.',
    );
  }

  private loadInvitationPreview(): void {
    this.http.get<FirmInvitationPreview>(`${this.api}/firm/invitations/preview`, {
      params: { token: this.invitationToken },
    }).subscribe({
      next: (preview) => {
        this.invitePreview = preview;
        this.email = preview.email;
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.showError(error, 'This invitation is invalid, expired, or has already been used.');
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadAccountingClients(): void {
    this.runRequest(
      this.http.get<AccountingClient[]>(`${this.api}/clients`),
      (clients) => {
        this.accountingClients = clients;
        if (!clients.some((client) => client.id === this.selectedAccountingClientId)) {
          this.selectedAccountingClientId = '';
        }
      },
      'Could not load accounting clients.',
    );
  }

  private loadOnboardingTemplates(): void {
    this.runRequest(
      this.http.get<OnboardingTemplate[]>(`${this.api}/onboarding-templates`),
      (templates) => {
        this.onboardingTemplates = templates;
        if (!templates.some((template) => template.id === this.selectedClientTemplateId)) {
          this.selectedClientTemplateId = '';
        }
        if (!templates.some((template) => template.id === this.editingTemplateId)) {
          this.resetTemplateEditor();
        }
      },
      'Could not load onboarding templates.',
    );
  }

  private loadSalesData(): void {
    this.runRequest(
      this.http.get<SalesCustomer[]>(`${this.api}/sales/customers`),
      (customers) => {
        this.salesCustomers = customers;
        if (!customers.some((customer) => customer.id === this.salesInvoiceCustomerId)) {
          this.salesInvoiceCustomerId = '';
        }
      },
      'Could not load sales customers.',
    );
    this.runRequest(
      this.http.get<SalesInvoice[]>(`${this.api}/sales/invoices`),
      (invoices) => {
        this.salesInvoices = invoices;
      },
      'Could not load sales invoices.',
    );
  }

  private loadPurchaseBills(): void {
    this.runRequest(
      this.http.get<PurchaseBill[]>(`${this.api}/purchases/bills`),
      (bills) => {
        this.purchaseBills = bills;
      },
      'Could not load supplier bills.',
    );
  }

  private loadTasks(): void {
    this.runRequest(
      this.http.get<Task[]>(
        `${this.api}/projects/${this.selectedProjectId}/workflows/${this.selectedWorkflowId}/tasks`,
      ),
      (tasks) => (this.tasks = tasks),
      'Could not load tasks.',
    );
  }

  private loadWorkflowRuns(): void {
    this.runRequest(
      this.http.get<WorkflowRun[]>(
        `${this.api}/projects/${this.selectedProjectId}/workflows/${this.selectedWorkflowId}/runs`,
      ),
      (workflowRuns) => (this.workflowRuns = workflowRuns),
      'Could not load workflow run history.',
    );
  }

  private loadAiUsage(): void {
    this.aiUsageLoading = true;
    this.aiUsageError = '';
    this.http.get<AiUsage>(`${this.api}/ai/usage`)
      .pipe(finalize(() => {
        this.aiUsageLoading = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: (usage) => {
          this.aiUsage = usage;
          this.changeDetector.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.aiUsageError = error.status === 401
            ? 'Your session expired. Sign in again to view AI usage.'
            : 'Could not load AI usage.';
          this.changeDetector.markForCheck();
        },
      });
  }

  private taskPath(task: Task): string {
    return `${this.api}/projects/${this.selectedProjectId}/workflows/${this.selectedWorkflowId}/tasks/${task.id}`;
  }

  private replaceTask(updated: Task): void {
    this.tasks = this.tasks.map((task) => (task.id === updated.id ? updated : task));
  }

  private replaceWorkflow(updated: Workflow): void {
    this.workflows = this.workflows.map((workflow) =>
      workflow.id === updated.id ? updated : workflow,
    );
  }

  private replaceAccountingClient(updated: AccountingClient): void {
    this.accountingClients = this.accountingClients.map((client) =>
      client.id === updated.id ? updated : client,
    );
  }

  private replaceSalesInvoice(updated: SalesInvoice): void {
    this.salesInvoices = this.salesInvoices.map((invoice) =>
      invoice.id === updated.id ? updated : invoice,
    );
  }

  private replacePurchaseBill(updated: PurchaseBill): void {
    this.purchaseBills = this.purchaseBills.map((bill) =>
      bill.id === updated.id ? updated : bill,
    );
  }

  private localDateString(date = new Date()): string {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return localDate.toISOString().slice(0, 10);
  }

  private runRequest<T>(
    request: Observable<T>,
    onSuccess: (value: T) => void,
    fallback: string,
  ): void {
    this.busy = true;
    request.pipe(finalize(() => {
      this.busy = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (value) => {
        onSuccess(value);
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.showError(error, fallback);
        this.changeDetector.markForCheck();
      },
    });
  }

  private showError(error: HttpErrorResponse, fallback: string): void {
    if (error.status === 401) {
      this.errorMessage = 'Unauthorized. Your session may have expired. Sign in again.';
      return;
    }
    const message = error.error?.detail || error.error?.message || error.error?.title;
    this.errorMessage = typeof message === 'string' ? message : fallback;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
