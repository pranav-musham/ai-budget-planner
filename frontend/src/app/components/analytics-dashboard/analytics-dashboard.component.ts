import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { Router } from '@angular/router';
import * as d3 from 'd3';
import { AnalyticsService, WeeklySpending, MonthlySpending, CategoryBreakdown } from '../../services/analytics.service';
import { TransactionService } from '../../services/transaction.service';
import { IncomeService } from '../../services/income.service';
import { IncomeSource } from '../../models/income.model';
import { Transaction } from '../../models/transaction.model';

interface MerchantStat {
  merchantName: string;
  totalAmount: number;
  transactionCount: number;
}

interface CategoryMerchantGroup {
  category: string;
  totalAmount: number;
  merchants: MerchantStat[];
}

interface MonthlySavingsData {
  month: string;
  income: number;
  expenses: number;
  savings: number;
}

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatExpansionModule
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.css'
})
export class AnalyticsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('weeklyChart') weeklyChartRef!: ElementRef;
  @ViewChild('monthlyChart') monthlyChartRef!: ElementRef;
  @ViewChild('categoryPieChart') categoryPieChartRef!: ElementRef;
  @ViewChild('incomeChart') incomeChartRef!: ElementRef;
  @ViewChild('savingsChart') savingsChartRef!: ElementRef;

  loading = true;
  weeklyData: WeeklySpending[] = [];
  monthlyData: MonthlySpending[] = [];
  categoryData: CategoryBreakdown[] = [];

  // Raw data for computations
  allTransactions: Transaction[] = [];
  incomeSources: IncomeSource[] = [];

  // Computed data
  categoryMerchantStats: CategoryMerchantGroup[] = [];
  monthlySavingsData: MonthlySavingsData[] = [];
  monthlyIncomeData: { month: string; amount: number }[] = [];

  // Summary stats
  totalSpending = 0;
  averageMonthlyExpenses = 0;
  totalIncome = 0;
  totalSavings = 0;

  constructor(
    private analyticsService: AnalyticsService,
    private transactionService: TransactionService,
    private incomeService: IncomeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  ngAfterViewInit(): void {
    // Charts will be rendered after data loads
  }

  ngOnDestroy(): void {
    this.clearCharts();
  }

  loadAllData(): void {
    this.loading = true;

    Promise.all([
      this.analyticsService.getWeeklySpending(8).toPromise(),
      this.analyticsService.getMonthlySpending(6).toPromise(),
      this.analyticsService.getCategoryBreakdown('MONTHLY').toPromise(),
      this.transactionService.getTransactions().toPromise(),
      this.incomeService.getIncomeSources().toPromise()
    ]).then(([weekly, monthly, category, transactions, incomeSources]) => {
      this.weeklyData = weekly || [];
      this.monthlyData = monthly || [];
      this.categoryData = category || [];
      this.allTransactions = transactions || [];
      this.incomeSources = incomeSources || [];

      // Summary stats
      this.totalSpending = this.allTransactions.reduce((sum, r) => sum + r.amount, 0);
      this.totalIncome = this.incomeSources.reduce((sum, i) => sum + i.amount, 0);
      this.totalSavings = this.totalIncome - this.totalSpending;

      this.computeAverageMonthlyExpenses();
      this.computeCategoryMerchantStats();
      this.computeMonthlyIncomeAndSavings();

      this.loading = false;
      setTimeout(() => this.renderAllCharts(), 100);
    }).catch(error => {
      console.error('Error loading analytics data:', error);
      this.loading = false;
    });
  }

  private computeAverageMonthlyExpenses(): void {
    const monthSet = new Set<string>();
    for (const t of this.allTransactions) {
      const d = new Date(t.transactionDate + 'T00:00:00');
      monthSet.add(`${d.getFullYear()}-${d.getMonth()}`);
    }
    const distinctMonths = monthSet.size;
    this.averageMonthlyExpenses = distinctMonths > 0 ? this.totalSpending / distinctMonths : 0;
  }

  private computeCategoryMerchantStats(): void {
    const catMap = new Map<string, Map<string, { amount: number; count: number }>>();

    for (const t of this.allTransactions) {
      const cat = t.category || 'Uncategorized';
      const merchant = t.merchantName || 'Unknown';

      if (!catMap.has(cat)) catMap.set(cat, new Map());
      const merchantMap = catMap.get(cat)!;
      if (!merchantMap.has(merchant)) merchantMap.set(merchant, { amount: 0, count: 0 });
      const entry = merchantMap.get(merchant)!;
      entry.amount += t.amount;
      entry.count++;
    }

    this.categoryMerchantStats = Array.from(catMap.entries())
      .map(([category, merchantMap]) => ({
        category,
        totalAmount: Array.from(merchantMap.values()).reduce((s, m) => s + m.amount, 0),
        merchants: Array.from(merchantMap.entries())
          .map(([merchantName, data]) => ({
            merchantName,
            totalAmount: data.amount,
            transactionCount: data.count
          }))
          .sort((a, b) => b.totalAmount - a.totalAmount)
      }))
      .sort((a, b) => b.totalAmount - a.totalAmount);
  }

  private computeMonthlyIncomeAndSavings(): void {
    const incomeByMonth = new Map<string, number>();
    for (const inc of this.incomeSources) {
      const d = new Date(inc.transactionDate + 'T00:00:00');
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      incomeByMonth.set(key, (incomeByMonth.get(key) || 0) + inc.amount);
    }

    const expenseByMonth = new Map<string, number>();
    for (const t of this.allTransactions) {
      const d = new Date(t.transactionDate + 'T00:00:00');
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      expenseByMonth.set(key, (expenseByMonth.get(key) || 0) + t.amount);
    }

    const allMonthKeys = new Set([...incomeByMonth.keys(), ...expenseByMonth.keys()]);
    const sortedKeys = Array.from(allMonthKeys).sort().slice(-6);

    this.monthlyIncomeData = sortedKeys.map(key => {
      const [year, month] = key.split('-').map(Number);
      const label = new Date(year, month - 1).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
      return { month: label, amount: incomeByMonth.get(key) || 0 };
    });

    this.monthlySavingsData = sortedKeys.map(key => {
      const [year, month] = key.split('-').map(Number);
      const label = new Date(year, month - 1).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
      const income = incomeByMonth.get(key) || 0;
      const expenses = expenseByMonth.get(key) || 0;
      return { month: label, income, expenses, savings: income - expenses };
    });
  }

  renderAllCharts(): void {
    if (this.weeklyChartRef && this.weeklyData.length > 0) {
      this.renderWeeklyChart();
    }
    if (this.monthlyChartRef && this.monthlyData.length > 0) {
      this.renderMonthlyChart();
    }
    if (this.categoryPieChartRef && this.categoryData.length > 0) {
      this.renderCategoryPieChart();
    }
    if (this.incomeChartRef && this.monthlyIncomeData.length > 0) {
      this.renderIncomeChart();
    }
    if (this.savingsChartRef && this.monthlySavingsData.length > 0) {
      this.renderSavingsChart();
    }
  }

  renderWeeklyChart(): void {
    const element = this.weeklyChartRef.nativeElement;
    d3.select(element).select('svg').remove();

    const margin = { top: 20, right: 30, bottom: 50, left: 60 };
    const width = element.offsetWidth - margin.left - margin.right;
    const height = 300 - margin.top - margin.bottom;

    const svg = d3.select(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand()
      .domain(this.weeklyData.map(d => d.week))
      .range([0, width])
      .padding(0.1);

    const y = d3.scaleLinear()
      .domain([0, d3.max(this.weeklyData, d => d.amount) || 0])
      .nice()
      .range([height, 0]);

    svg.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-45)');

    svg.append('g')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `$${d}`));

    const line = d3.line<WeeklySpending>()
      .x(d => (x(d.week) || 0) + x.bandwidth() / 2)
      .y(d => y(d.amount))
      .curve(d3.curveMonotoneX);

    svg.append('path')
      .datum(this.weeklyData)
      .attr('fill', 'none')
      .attr('stroke', '#2563eb')
      .attr('stroke-width', 3)
      .attr('d', line);

    svg.selectAll('.dot')
      .data(this.weeklyData)
      .enter().append('circle')
      .attr('class', 'dot')
      .attr('cx', d => (x(d.week) || 0) + x.bandwidth() / 2)
      .attr('cy', d => y(d.amount))
      .attr('r', 5)
      .attr('fill', '#2563eb')
      .append('title')
      .text(d => `${d.week}: $${d.amount.toFixed(2)}`);
  }

  renderMonthlyChart(): void {
    const element = this.monthlyChartRef.nativeElement;
    d3.select(element).select('svg').remove();

    const margin = { top: 20, right: 30, bottom: 50, left: 60 };
    const width = element.offsetWidth - margin.left - margin.right;
    const height = 300 - margin.top - margin.bottom;

    const svg = d3.select(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand()
      .domain(this.monthlyData.map(d => d.month))
      .range([0, width])
      .padding(0.2);

    const y = d3.scaleLinear()
      .domain([0, d3.max(this.monthlyData, d => d.amount) || 0])
      .nice()
      .range([height, 0]);

    svg.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-45)');

    svg.append('g')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `$${d}`));

    svg.selectAll('.bar')
      .data(this.monthlyData)
      .enter().append('rect')
      .attr('class', 'bar')
      .attr('x', d => x(d.month) || 0)
      .attr('y', d => y(d.amount))
      .attr('width', x.bandwidth())
      .attr('height', d => height - y(d.amount))
      .attr('fill', '#8b5cf6')
      .append('title')
      .text(d => `${d.month}: $${d.amount.toFixed(2)}`);
  }

  renderCategoryPieChart(): void {
    const element = this.categoryPieChartRef.nativeElement;
    d3.select(element).select('svg').remove();

    const width = element.offsetWidth;
    const height = 350;
    const radius = Math.min(width, height) / 2 - 40;

    const svg = d3.select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .append('g')
      .attr('transform', `translate(${width / 2},${height / 2})`);

    const color = d3.scaleOrdinal<string>()
      .domain(this.categoryData.map(d => d.category))
      .range(['#2563eb', '#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#3b82f6', '#06b6d4', '#8b5cf6']);

    const pie = d3.pie<CategoryBreakdown>()
      .value(d => d.amount)
      .sort(null);

    const arc = d3.arc<d3.PieArcDatum<CategoryBreakdown>>()
      .innerRadius(0)
      .outerRadius(radius);

    const arcs = svg.selectAll('.arc')
      .data(pie(this.categoryData))
      .enter().append('g')
      .attr('class', 'arc');

    arcs.append('path')
      .attr('d', arc)
      .attr('fill', d => color(d.data.category))
      .attr('stroke', 'white')
      .attr('stroke-width', 2)
      .append('title')
      .text(d => `${d.data.category}: $${d.data.amount.toFixed(2)} (${d.data.percentage.toFixed(1)}%)`);

    arcs.append('text')
      .attr('transform', d => `translate(${arc.centroid(d)})`)
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('fill', 'white')
      .text(d => d.data.percentage > 5 ? `${d.data.percentage.toFixed(0)}%` : '');
  }

  renderIncomeChart(): void {
    const element = this.incomeChartRef.nativeElement;
    d3.select(element).select('svg').remove();

    const margin = { top: 20, right: 30, bottom: 50, left: 60 };
    const width = element.offsetWidth - margin.left - margin.right;
    const height = 300 - margin.top - margin.bottom;

    const svg = d3.select(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand()
      .domain(this.monthlyIncomeData.map(d => d.month))
      .range([0, width])
      .padding(0.2);

    const y = d3.scaleLinear()
      .domain([0, d3.max(this.monthlyIncomeData, d => d.amount) || 0])
      .nice()
      .range([height, 0]);

    svg.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-45)');

    svg.append('g')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `$${d}`));

    svg.selectAll('.bar')
      .data(this.monthlyIncomeData)
      .enter().append('rect')
      .attr('class', 'bar')
      .attr('x', d => x(d.month) || 0)
      .attr('y', d => y(d.amount))
      .attr('width', x.bandwidth())
      .attr('height', d => height - y(d.amount))
      .attr('fill', '#10b981')
      .append('title')
      .text(d => `${d.month}: $${d.amount.toFixed(2)}`);
  }

  renderSavingsChart(): void {
    const element = this.savingsChartRef.nativeElement;
    d3.select(element).select('svg').remove();

    const margin = { top: 20, right: 30, bottom: 50, left: 60 };
    const width = element.offsetWidth - margin.left - margin.right;
    const height = 300 - margin.top - margin.bottom;

    const svg = d3.select(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand()
      .domain(this.monthlySavingsData.map(d => d.month))
      .range([0, width])
      .padding(0.2);

    const minSavings = d3.min(this.monthlySavingsData, d => d.savings) || 0;
    const maxSavings = d3.max(this.monthlySavingsData, d => d.savings) || 0;

    const y = d3.scaleLinear()
      .domain([Math.min(0, minSavings), Math.max(0, maxSavings)])
      .nice()
      .range([height, 0]);

    svg.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-45)');

    svg.append('g')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `$${d}`));

    // Zero line
    svg.append('line')
      .attr('x1', 0)
      .attr('x2', width)
      .attr('y1', y(0))
      .attr('y2', y(0))
      .attr('stroke', '#9ca3af')
      .attr('stroke-dasharray', '4,4')
      .attr('stroke-width', 1);

    svg.selectAll('.bar')
      .data(this.monthlySavingsData)
      .enter().append('rect')
      .attr('class', 'bar')
      .attr('x', d => x(d.month) || 0)
      .attr('y', d => d.savings >= 0 ? y(d.savings) : y(0))
      .attr('width', x.bandwidth())
      .attr('height', d => Math.abs(y(0) - y(d.savings)))
      .attr('fill', d => d.savings >= 0 ? '#2563eb' : '#ef4444')
      .append('title')
      .text(d => `${d.month}: $${d.savings.toFixed(2)} (Income: $${d.income.toFixed(2)}, Expenses: $${d.expenses.toFixed(2)})`);
  }

  clearCharts(): void {
    if (this.weeklyChartRef) d3.select(this.weeklyChartRef.nativeElement).select('svg').remove();
    if (this.monthlyChartRef) d3.select(this.monthlyChartRef.nativeElement).select('svg').remove();
    if (this.categoryPieChartRef) d3.select(this.categoryPieChartRef.nativeElement).select('svg').remove();
    if (this.incomeChartRef) d3.select(this.incomeChartRef.nativeElement).select('svg').remove();
    if (this.savingsChartRef) d3.select(this.savingsChartRef.nativeElement).select('svg').remove();
  }

  navigateToAddExpense(): void {
    this.router.navigate(['/transactions/create']);
  }

  navigateToReceipts(): void {
    this.router.navigate(['/transactions']);
  }

  navigateToBudgets(): void {
    this.router.navigate(['/budgets/settings']);
  }

  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'Groceries': '#2563eb',
      'Dining': '#8b5cf6',
      'Transportation': '#ec4899',
      'Health': '#10b981',
      'Shopping': '#f59e0b',
      'Entertainment': '#3b82f6',
      'Bills': '#06b6d4',
      'Travel': '#8b5cf6',
      'Education': '#10b981',
      'Other': '#6b7280'
    };
    return colors[category] || '#9ca3af';
  }
}
